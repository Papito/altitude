package software.altitude.core.service

import com.drew.imaging.ImageMetadataReader
import com.drew.metadata.exif.ExifDirectoryBase
import com.drew.metadata.exif.ExifIFD0Directory
import org.imgscalr.Scalr
import org.opencv.core.Mat
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import play.api.libs.json.JsObject
import software.altitude.core.Altitude
import software.altitude.core.RequestContext
import software.altitude.core.dao.jdbc.BaseDao
import software.altitude.core.models.Folder
import software.altitude.core.models._
import software.altitude.core.service.FaceService.matFromBytes
import software.altitude.core.transactions.TransactionManager
import software.altitude.core.util.Query
import software.altitude.core.util.QueryResult
import software.altitude.core.util.SearchQuery
import software.altitude.core.util.SearchResult
import software.altitude.core.{Const => C, _}

import java.awt.AlphaComposite
import java.awt.Color
import java.awt.Graphics2D
import java.awt.geom.AffineTransform
import java.awt.image.AffineTransformOp
import java.awt.image.BufferedImage
import java.awt.image.ColorModel
import java.io._
import javax.imageio.ImageIO


/**
  * The class that stitches it all together
  * TOC:
  * - ASSETS
  * - DATA/PREVIEW
  * - DISCOVERY
  * - FOLDERS
  * - RECYCLING
  * - METADATA
  */
class LibraryService(val app: Altitude) {
  protected final val logger: Logger = LoggerFactory.getLogger(getClass)
  protected val txManager: TransactionManager = app.txManager

  private val previewBoxSize: Int = app.config.getInt(C.Conf.PREVIEW_BOX_PIXELS)

  /** **************************************************************************
    * ASSETS
    * *********************************************************************** */

  def add(assetIn: Asset): JsObject = {
    logger.info(s"Preparing to add asset [$assetIn]")

    txManager.withTransaction[JsObject] {
      val existing: Option[Asset] = getByChecksum(assetIn.checksum)

      if (existing.nonEmpty) {
        logger.debug(s"Duplicate found for [$assetIn] and checksum: ${assetIn.checksum}")
        throw DuplicateException()
      }

      /**
        * Process metadata and append it to the asset
        */
      val metadata = app.service.metadata.cleanAndValidate(assetIn.metadata)

      val assetId = BaseDao.genId

      val assetToAddModel: Asset = assetIn.copy(
        id = Some(assetId),
        metadata = metadata,
        data = assetIn.data,
        extractedMetadata = assetIn.extractedMetadata
      )

      logger.info(s"Adding asset: $assetToAddModel")

      // FIXME: this is temporary, to work under tests
      if (assetIn.data.nonEmpty) {
        val image: Mat = matFromBytes(assetIn.data)
        val results: List[Mat] = app.service.face.detectFacesWithYunet(image)
        results.indices.foreach { idx =>
          val res = results(idx)
          val rect = FaceService.faceDetectToRect(res)
          logger.info("Face detected: " + rect)
        }
      }

      app.service.asset.add(assetToAddModel)

      app.service.search.indexAsset(assetToAddModel)

      app.service.stats.addAsset(assetToAddModel)

      addPreview(assetToAddModel)

      app.service.fileStore.addAsset(assetToAddModel)

      assetToAddModel
    }
  }

  def deleteById(id: String): Unit = {
    throw new NotImplementedError
  }

  def getById(id: String): JsObject = {
    txManager.asReadOnly[JsObject] {
      app.service.asset.getById(id)
    }
  }

  def getByChecksum(checksum: String): Option[Asset] = {
    txManager.asReadOnly[Option[Asset]] {
      val query = new Query(params = Map(C.Asset.CHECKSUM -> checksum)).withRepository()
      val existing = app.service.asset.query(query)
      if (existing.nonEmpty) Some(existing.records.head: Asset) else None
    }
  }

  def moveAssetToFolder(assetId: String, folderId: String)
                       : Asset = {
    txManager.withTransaction[Asset] {
      moveAssetsToFolder(Set(assetId), folderId)
      getById(assetId)
    }
  }

  def moveAssetsToFolder(assetIds: Set[String], destFolderId: String): Unit = {

    def move(asset: Asset): Unit = {
      // Cannot move to the same folder
      // Note that a recycled asset CAN be restored to its original folder
      if (!asset.isRecycled && asset.folderId == destFolderId) {
        return
      }

      app.service.stats.moveAsset(asset, destFolderId)

      /* Point the asset to the new folder.
         It may or may not be recycled or triaged, so we update it as neither unconditionally
         (saves us a separate update query)
      */
      val updatedAsset: Asset = asset.copy(
        folderId = destFolderId,
        isRecycled = false,
        isTriaged = false)

      app.service.asset.updateById(
        asset.persistedId, updatedAsset,
        fields = List(C.Asset.FOLDER_ID, C.Asset.IS_RECYCLED, C.Asset.IS_TRIAGED))

    }

    txManager.withTransaction {
      // ensure the folder exists
      app.service.folder.getById(destFolderId)

      assetIds.foreach { assetId =>
        val asset: Asset = getById(assetId)

        move(asset)
      }
    }
  }

  def renameAsset(assetId: String, newFilename: String): Asset = {

    txManager.withTransaction[Asset] {
      val asset: Asset = getById(assetId)

      if (asset.isRecycled) {
        throw IllegalOperationException(s"Cannot rename a recycled asset: [$asset]")
      }

      val updatedAsset: Asset = asset.copy(fileName = newFilename)

      // Note that we are not updating the PATH because it does not exist as a property
      app.service.asset.updateById(
        asset.persistedId,
        updatedAsset,
        fields = List(C.Asset.FILENAME))

      updatedAsset
    }
  }

  /** **************************************************************************
    * DATA/PREVIEW
    * *********************************************************************** */

  private def genPreviewData(asset: Asset): Array[Byte] = {
    asset.assetType.mediaType match {
      case "image" =>
        makeImageThumbnail(asset)
      case _ => new Array[Byte](0)
    }
  }

  private def addPreview(asset: Asset): Option[Preview] = {
    val previewData: Array[Byte] = genPreviewData(asset)

    previewData.length match {
      case size if size > 0 =>

        val preview: Preview = Preview(
          assetId = asset.persistedId,
          mimeType = asset.assetType.mime,
          data = previewData)

        app.service.fileStore.addPreview(preview)

        Some(preview)
      case _ => None
    }
  }

  private def makeImageThumbnail(asset: Asset): Array[Byte] = {
    try {
      val mt: com.drew.metadata.Metadata = ImageMetadataReader.readMetadata(new ByteArrayInputStream(asset.data))
      val exifDirectory = mt.getFirstDirectoryOfType(classOf[ExifIFD0Directory])
      // val jpegDirectory = mt.getFirstDirectoryOfType(classOf[JpegDirectory])

      val orientation: Int = try {
        exifDirectory.getInt(ExifDirectoryBase.TAG_ORIENTATION)
      } catch {
        case _: Exception => 1
      }

      /**
       * Rotate the image if necessary
       *
       * https://sirv.com/help/articles/rotate-photos-to-be-upright/
       * https://stackoverflow.com/questions/5905868/how-to-rotate-jpeg-images-based-on-the-orientation-metadata
       */
      val dataStream: InputStream = new ByteArrayInputStream(asset.data)
      val srcImage: BufferedImage = ImageIO.read(dataStream)
      val scaledImage: BufferedImage = Scalr.resize(srcImage, Scalr.Method.QUALITY, previewBoxSize)

      val width = scaledImage.getWidth
      val height = scaledImage.getHeight

      val transform: AffineTransform = new AffineTransform()
      orientation match {
        case 1 =>
        case 2 =>
          transform.scale(-1.0, 1.0);
          transform.translate(-width, 0);
        case 3 =>
          transform.translate(width, height);
          transform.rotate(Math.PI);
        case 4 =>
          transform.scale(1.0, -1.0);
          transform.translate(0, -height);
        case 5 =>
          transform.rotate(-Math.PI / 2);
          transform.scale(-1.0, 1.0);
        case 6 =>
          transform.translate(height, 0);
          transform.rotate(Math.PI / 2);
        case 7 =>
          transform.scale(-1.0, 1.0);
          transform.translate(-height, 0);
          transform.translate(0, width);
          transform.rotate(3 * Math.PI / 2);
        case 8 =>
          transform.translate(0, width);
          transform.rotate(3 * Math.PI / 2);
        case _ =>
      }

      val op = new AffineTransformOp(transform, AffineTransformOp.TYPE_BICUBIC)

      val colorModel: ColorModel = scaledImage.getType match {
        case BufferedImage.TYPE_BYTE_GRAY => null
        case _ => scaledImage.getColorModel
      }

      val destinationImage = op.createCompatibleDestImage(scaledImage, colorModel)

      val graphics = destinationImage.createGraphics()
      graphics.setBackground(Color.WHITE)
      graphics.clearRect(0, 0, destinationImage.getWidth, destinationImage.getHeight)
      val rotationCorrectScaledImage = op.filter(scaledImage, destinationImage)

      val compositeImage: BufferedImage =
        new BufferedImage(previewBoxSize, previewBoxSize, BufferedImage.TYPE_INT_ARGB)
      val G2D: Graphics2D = compositeImage.createGraphics

      val x: Int = if (rotationCorrectScaledImage.getHeight > rotationCorrectScaledImage.getWidth) {
        (previewBoxSize - rotationCorrectScaledImage.getWidth) / 2
      } else 0
      val y: Int = if (rotationCorrectScaledImage.getHeight < rotationCorrectScaledImage.getWidth) {
        (previewBoxSize - rotationCorrectScaledImage.getHeight()) / 2
      } else 0

      G2D.setComposite(AlphaComposite.Clear)
      G2D.fillRect(0, 0, previewBoxSize, previewBoxSize)
      G2D.setComposite(AlphaComposite.Src)
      G2D.drawImage(rotationCorrectScaledImage, x, y, null)
      val byteArray: ByteArrayOutputStream = new ByteArrayOutputStream
      ImageIO.write(compositeImage, "png", byteArray)
      graphics.dispose()

      byteArray.toByteArray

    } catch {
      case ex: Exception =>
        logger.error(s"Error generating preview for $asset")
        software.altitude.core.Util.logStacktrace(ex)
        throw FormatException(asset)
    }
  }

  def getPreview(assetId: String): Preview = {
    app.service.fileStore.getPreviewById(assetId)
  }

  /** **************************************************************************
    * DISCOVERY
    * *********************************************************************** */

  def query(query: Query): QueryResult = {
    txManager.asReadOnly[QueryResult] {
      val folderId = query.params.get(C.Asset.FOLDER_ID).asInstanceOf[Option[String]]

      val _query: Query = if (folderId.isDefined) {
        val allFolderIds = app.service.folder.flatChildrenIds(parentIds = Set(folderId.get))
        query.add(C.Asset.FOLDER_ID ->  Query.IN(allFolderIds.asInstanceOf[Set[Any]]))
      }
      else {
        query
      }

      app.service.asset.query(_query.withRepository())
    }
  }

  def search(query: SearchQuery): SearchResult = {
    txManager.asReadOnly[SearchResult] {
      val _query: SearchQuery = if (query.folderIds.nonEmpty) {
        // create a new query, with the new folder set
        val allFolderIds = app.service.folder.flatChildrenIds(parentIds = query.folderIds)

        new SearchQuery(text = query.text,
          folderIds = allFolderIds,
          params = query.params,
          rpp = query.rpp,
          page = query.page)
      }
      else {
        query
      }

      app.service.search.search(_query)
    }
  }

  def queryRecycled(query: Query): QueryResult = {
    app.service.asset.queryRecycled(query)
  }

  def queryAll(query: Query): QueryResult = {
    app.service.asset.queryAll(query)
  }


  /** **************************************************************************
    * FOLDERS
    * *********************************************************************** */

  def addFolder(name: String, parentId: Option[String] = None): Folder = {
    txManager.withTransaction[JsObject] {
      val _parentId = if (parentId.isDefined) parentId.get else RequestContext.repository.value.get.rootFolderId
      val folder = Folder(name = name.trim, parentId = _parentId)
      val addedFolder: Folder = app.service.folder.add(folder)

      app.service.folder.incrChildCount(_parentId)

      addedFolder
    }
  }

  /**
    * Delete a folder by ID, including its children. Does not allow deleting the root folder,
    * or any system folders.
    *
    * Recycle all the assets in the tree.
    */
  def deleteFolderById(id: String): Unit = {
    if (app.service.folder.isRootFolder(id)) {
      throw IllegalOperationException("Cannot delete the root folder")
    }

    txManager.withTransaction {
      val folder: Folder = app.service.folder.getById(id)

      logger.info(s"Deleting folder $folder")

      // get the list of tuples - (depth, id), with most-deep first
      val childrenAndDepths: List[(Int, String)] =
        app.service.folder.flatChildrenIdsWithDepths(
          id, app.service.folder.repositoryFolders()).sortBy(_._1).reverse
      /* ^^^ sort by depth */


      logger.trace(s"Folder children, deepest first: $childrenAndDepths")

      childrenAndDepths.foldLeft(0) { (assetCount: Int, f: (Int, String)) =>
        val folderId = f._2
        val childFolder: Folder = app.service.folder.getById(folderId)

        logger.trace(s"Deleting or recycling folder $f")

        // set all the assets as recycled
        val folderAssetsQuery = new Query(Map(C.Asset.FOLDER_ID -> folderId))

        val results: QueryResult = app.service.library.queryAll(folderAssetsQuery)

        logger.trace(s"Folder $folderId has ${results.total} assets")

        // OPTIMIZE: bulk deletions.
        results.records.foreach { record =>
          val asset: Asset = record

          /* We only want to recycle assets that are not recycled,
             however the query is for all assets, as we need to know if the folder
             has any asset references in the repository. This spares us a second
             query.
           */
          if (!asset.isRecycled) this.recycleAsset(asset.persistedId)
        }

        val treeAssetCount = assetCount + results.total

        logger.trace(s"Total assets in the tree of folder ${folder.persistedId}: $treeAssetCount")

        // delete folder if it has no assets, otherwise - recycle it
        if (treeAssetCount == 0) {
          logger.trace(s"DELETING (PURGING) folder $f")
          app.service.folder.deleteById(folderId)
        }
        else {
          logger.trace(s"RECYCLING folder $folderId")
          app.service.folder.setRecycledProp(childFolder, isRecycled = true)
        }

        treeAssetCount // accumulates total asset count for the next step in the fold
      }

      app.service.folder.decrChildCount(folder.parentId)
    }
  }

  def renameFolder(folderId: String, newName: String): Folder = {
    txManager.withTransaction[Folder] {
      val updatedFolder = app.service.folder.rename(folderId, newName)
      updatedFolder
    }
  }

  def moveFolder(folderBeingMovedId: String, destFolderId: String)
                : Folder = {
    txManager.withTransaction[Folder] {
      val (movedFolder, _) = app.service.folder.move(folderBeingMovedId, destFolderId)
      movedFolder
    }
  }


  /** **************************************************************************
    * RECYCLING
    * *********************************************************************** */

  def restoreRecycledAsset(assetId: String): Asset = {
    txManager.withTransaction[Asset] {
      restoreRecycledAssets(Set(assetId))
      getById(assetId)
    }
  }

  def restoreRecycledAssets(assetIds: Set[String]): Unit = {
    logger.info(s"Restoring recycled assets [${assetIds.mkString(",")}]")

    assetIds.foreach { assetId =>
      logger.info(s"Restoring recycled asset [$assetId]")

      val asset: Asset = getById(assetId)
      val existing = getByChecksum(asset.checksum)

      if (existing.isDefined) {
        throw DuplicateException()
      }

      txManager.withTransaction {
        if (asset.isRecycled) {
          app.service.asset.setRecycledProp(asset, isRecycled = false)
          // OPTIMIZE: create a lookup cache for folders, to avoid querying for each asset
          val folder: Folder = app.service.folder.getById(asset.folderId)

          if (folder.isRecycled) {
            app.service.folder.setRecycledProp(folder = folder, isRecycled = false)
          }

          val restoredAsset: Asset = getById(assetId)
          app.service.stats.restoreAsset(restoredAsset)
        }
      }
    }
  }

  def recycleAsset(assetId: String): Asset = {
    txManager.withTransaction {
      recycleAssets(Set(assetId))
      getById(assetId)
    }
  }

  def recycleAssets(assetIds: Set[String]): Unit = {

    assetIds.foreach { assetId =>
      txManager.withTransaction {
        val asset: Asset = getById(assetId)
        app.service.asset.setRecycledProp(asset, isRecycled = true)
        app.service.stats.recycleAsset(asset.copy(isRecycled = true))
      }
    }
  }

  /** **************************************************************************
    * METADATA
    * *********************************************************************** */

  def addMetadataValue(assetId: String, fieldId: String, newValue: Any): Unit = {
    txManager.withTransaction {
      app.service.metadata.addFieldValue(assetId, fieldId, newValue.toString)
      val field: MetadataField = app.service.metadata.getFieldById(fieldId)
      val asset: Asset = getById(assetId)
      app.service.search.addMetadataValue(asset, field, newValue.toString)
    }
  }

  def deleteMetadataValue(assetId: String, valueId: String): Unit = {
    txManager.withTransaction {
      app.service.metadata.deleteFieldValue(assetId, valueId)
      val asset: Asset = getById(assetId)
      // OPTIMIZE: store value ID with search to delete in a targeted way
      app.service.search.reindexAsset(asset)
    }
  }

  def updateMetadataValue(assetId: String, valueId: String, newValue: Any): Unit = {

    txManager.withTransaction {
      app.service.metadata.updateFieldValue(assetId, valueId, newValue.toString)
      val asset: Asset = getById(assetId)
      // OPTIMIZE: store value ID with search to update in a targeted way
      app.service.search.reindexAsset(asset)
    }
  }

}
