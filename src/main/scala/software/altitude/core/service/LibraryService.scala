package software.altitude.core.service

import org.imgscalr.Scalr
import org.slf4j.LoggerFactory
import play.api.libs.json.JsObject
import software.altitude.core.Altitude
import software.altitude.core.RequestContext
import software.altitude.core.dao.jdbc.BaseDao
import software.altitude.core.models.Folder
import software.altitude.core.models._
import software.altitude.core.transactions.TransactionManager
import software.altitude.core.util.Query
import software.altitude.core.util.QueryResult
import software.altitude.core.util.SearchQuery
import software.altitude.core.util.SearchResult
import software.altitude.core.{Const => C, _}

import java.awt.AlphaComposite
import java.awt.Graphics2D
import java.awt.image.BufferedImage
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
  private final val log = LoggerFactory.getLogger(getClass)
  protected val txManager: TransactionManager = app.txManager

  private val previewBoxSize: Int = app.config.getInt("preview.box.pixels")

  /** **************************************************************************
    * ASSETS
    * *********************************************************************** */

  def add(assetIn: Asset): JsObject = {
    log.info(s"Preparing to add asset [$assetIn]")

    if (app.service.folder.isRootFolder(assetIn.folderId)) {
      throw IllegalOperationException("Cannot have assets in root folder")
    }

    txManager.withTransaction[JsObject] {
      val existing: Option[Asset] = getByChecksum(assetIn.checksum)

      if (existing.nonEmpty) {
        log.debug(s"Duplicate found for [$assetIn] and checksum: ${assetIn.checksum}")
        val existingAsset: Asset = existing.get
        throw DuplicateException(existingAsset.id.get)
      }

      /**
        * Process metadata and append it to the asset
        */
      val metadata = app.service.metadata.cleanAndValidate(assetIn.metadata)
      val fileName = app.service.fileStore.calculateNextAvailableFilename(assetIn)

      val assetId = BaseDao.genId

      val assetToAddModel: Asset = assetIn.copy(
        id = Some(assetId),
        fileName = fileName,
        metadata = metadata,
        data = assetIn.data,
        extractedMetadata = assetIn.extractedMetadata
      )

      log.info(s"Adding asset: $assetToAddModel")

      app.service.asset.add(assetToAddModel)

      app.service.search.indexAsset(assetToAddModel)

      app.service.stats.addAsset(assetToAddModel)

      addPreview(assetToAddModel)

      app.service.fileStore.addAsset(assetToAddModel)

      val path = app.service.fileStore.getAssetPath(assetToAddModel)
      assetToAddModel.copy(path = Some(path))
    }
  }

  def deleteById(id: String): Unit = {
    throw new NotImplementedError
  }

  def getById(id: String): JsObject = {
    txManager.asReadOnly[JsObject] {
      val asset: Asset = app.service.asset.getById(id)
      val path = app.service.fileStore.getAssetPath(asset)
      asset.copy(path = Some(path))
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

  def moveAssetToTriage(assetId: String): Unit = {
    txManager.withTransaction {
      moveAssetsToTriage(Set(assetId))
    }
  }

  def moveAssetsToTriage(assetIds: Set[String]): Unit = {
    txManager.withTransaction {
      moveAssetsToFolder(assetIds, RequestContext.repository.value.get.triageFolderId)
    }
  }

  def moveAssetsToFolder(assetIds: Set[String], destFolderId: String): Unit = {

    def move(asset: Asset): Unit = {
      // cannot move to the same folder
      if (!asset.isRecycled && asset.folderId == destFolderId) {
        return
      }

      app.service.stats.moveAsset(asset, destFolderId)

      /* Point the asset to the new folder.
         It may or may not be recycled, so we update it as not recycled unconditionally
         (saves us a separate update query)
      */
      val updatedAsset: Asset = asset.copy(
        folderId = destFolderId,
        isRecycled = false)

      app.service.asset.updateById(
        asset.id.get, updatedAsset,
        fields = List(C.Asset.FOLDER_ID, C.Asset.IS_RECYCLED))

      app.service.fileStore.moveAsset(asset, updatedAsset)
    }

    txManager.withTransaction {
      // ensure the folder exists
      app.service.folder.getById(destFolderId)

      assetIds.foreach { assetId =>
        // cannot have assets in root folder - just other folders
        if (app.service.folder.isRootFolder(destFolderId)) {
          throw IllegalOperationException("Cannot move assets to root folder")
        }

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

      val path = app.service.fileStore.getPathWithNewFilename(asset, newFilename)
      val updatedAsset: Asset = asset.copy(
        fileName = newFilename,
        path = Some(path))

      // Note that we are not updating the PATH because it does not exist as a property
      app.service.asset.updateById(
        asset.id.get, updatedAsset,
        fields = List(C.Asset.FILENAME))

      app.service.fileStore.moveAsset(asset, updatedAsset)

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
    require(asset.id.nonEmpty, "Asset ID cannot be empty")

    val previewData: Array[Byte] = genPreviewData(asset)

    previewData.length match {
      case size if size > 0 =>

        val preview: Preview = Preview(
          assetId = asset.id.get,
          mimeType = asset.assetType.mime,
          data = previewData)

        app.service.fileStore.addPreview(preview)

        Some(preview)
      case _ => None
    }
  }

  // FIXME: this is temporary as only image-specific
  private def makeImageThumbnail(asset: Asset): Array[Byte] = {
    try {
      require(asset.data.length != 0, "File length cannot be zero")
      val dataStream: InputStream = new ByteArrayInputStream(asset.data)
      val srcImage: BufferedImage = ImageIO.read(dataStream)
      val scaledImage: BufferedImage = Scalr.resize(srcImage, Scalr.Method.ULTRA_QUALITY, previewBoxSize)
      val height: Int = scaledImage.getHeight
      val width: Int = scaledImage.getWidth

      val x: Int = if (height > width) (previewBoxSize - width) / 2 else 0

      val y: Int = if (height < width) (previewBoxSize - height) / 2 else 0

      val COMPOSITE_IMAGE: BufferedImage =
        new BufferedImage(previewBoxSize, previewBoxSize, BufferedImage.TYPE_INT_ARGB)
      val G2D: Graphics2D = COMPOSITE_IMAGE.createGraphics

      G2D.setComposite(AlphaComposite.Clear)
      G2D.fillRect(0, 0, previewBoxSize, previewBoxSize)
      G2D.setComposite(AlphaComposite.Src)
      G2D.drawImage(scaledImage, x, y, null)
      val byteArray: ByteArrayOutputStream = new ByteArrayOutputStream
      ImageIO.write(COMPOSITE_IMAGE, "png", byteArray)

      byteArray.toByteArray
    } catch {
      case ex: Exception =>
        log.error(s"Error generating preview for $asset")
        software.altitude.core.Util.logStacktrace(ex)
        throw FormatException(asset)
    }
  }

  def getPreview(assetId: String): Preview = {
    require(assetId.nonEmpty, "Asset ID cannot be empty")
    app.service.fileStore.getPreviewById(assetId)
  }

  def getData(assetId: String): Data = {
    app.service.fileStore.getById(assetId)
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

  def addFolder(name: String, parentId: Option[String] = None): JsObject = {
    txManager.withTransaction[JsObject] {
      val _parentId = if (parentId.isDefined) parentId.get else RequestContext.repository.value.get.rootFolderId
      val folder = Folder(name = name.trim, parentId = _parentId)
      val addedFolder = app.service.folder.add(folder)
      app.service.fileStore.addFolder(addedFolder)
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

    if (app.service.folder.isSystemFolder(Some(id))) {
      throw IllegalOperationException("Cannot delete system folder")
    }

    txManager.withTransaction {
      val folder: Folder = app.service.folder.getById(id)

      log.info(s"Deleting folder $folder")

      // get the list of tuples - (depth, id), with most-deep first
      val childrenAndDepths: List[(Int, String)] =
        app.service.folder.flatChildrenIdsWithDepths(
          id, app.service.folder.repositoryFolders()).sortBy(_._1).reverse
      /* ^^^ sort by depth */


      log.trace(s"Folder children, deepest first: $childrenAndDepths")

      childrenAndDepths.foldLeft(0) { (assetCount: Int, f: (Int, String)) =>
        val folderId = f._2
        val childFolder: Folder = app.service.folder.getById(folderId)

        log.trace(s"Deleting or recycling folder $f")

        // set all the assets as recycled
        val folderAssetsQuery = new Query(Map(C.Asset.FOLDER_ID -> folderId))

        val results: QueryResult = app.service.library.queryAll(folderAssetsQuery)

        log.trace(s"Folder ${folderId} has ${results.total} assets")

        // OPTIMIZE: bulk deletions.
        results.records.foreach { record =>
          val asset: Asset = record

          /* We only want to recycle assets that are not recycled,
             however the query is for all assets, as we need to know if the folder
             has any asset references in the repository. This spares us a second
             query.
           */
          if (!asset.isRecycled) this.recycleAsset(asset.id.get)
        }

        val treeAssetCount = assetCount + results.total

        log.trace(s"Total assets in the tree of folder ${folder.id.get}: $treeAssetCount")

        // delete folder if it has no assets, otherwise - recycle it
        if (treeAssetCount == 0) {
          log.trace(s"DELETING (PURGING) folder $f")
          app.service.folder.deleteById(folderId)
          app.service.fileStore.deleteFolder(childFolder)
        }
        else {
          log.trace(s"RECYCLING folder $folderId")
          app.service.folder.setRecycledProp(childFolder, isRecycled = true)
        }

        treeAssetCount // accumulates total asset count for the next step in the fold
      }
    }
  }

  def renameFolder(folderId: String, newName: String): Folder = {
    txManager.withTransaction[Folder] {
      val folder: Folder = app.service.folder.getById(folderId)
      val updatedFolder = app.service.folder.rename(folderId, newName)
      app.service.fileStore.renameFolder(folder, newName)
      updatedFolder
    }
  }

  def moveFolder(folderBeingMovedId: String, destFolderId: String)
                : Folder = {
    txManager.withTransaction[Folder] {
      val (movedFolder, newParent) = app.service.folder.move(folderBeingMovedId, destFolderId)
      app.service.fileStore.moveFolder(movedFolder, newParent)
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
    log.info(s"Restoring recycled assets [${assetIds.mkString(",")}]")

    assetIds.foreach { assetId =>
      log.info(s"Restoring recycled asset [$assetId]")

      val asset: Asset = getById(assetId)
      val existing = getByChecksum(asset.checksum)

      if (existing.isDefined) {
        throw DuplicateException(existingAssetId = existing.get.id.get)
      }

      txManager.withTransaction {
        if (asset.isRecycled) {
          app.service.asset.setRecycledProp(asset, isRecycled = false)
          // OPTIMIZE: create a lookup cache for folders, to avoid querying for each asset
          val folder: Folder = app.service.folder.getById(asset.folderId)

          if (folder.isRecycled) {
            app.service.folder.setRecycledProp(folder = folder, isRecycled = false)
            // the folder is not in the store.
            app.service.fileStore.addFolder(folder)
          }

          val restoredAsset: Asset = getById(assetId)
          app.service.stats.restoreAsset(restoredAsset)
          app.service.fileStore.restoreAsset(restoredAsset)
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

  def recycleFolder(folderId: String): Folder = {

    txManager.withTransaction {
      val folder: Folder = app.service.folder.getById(folderId)
      app.service.folder.setRecycledProp(folder, isRecycled = true)
      folder.copy(isRecycled = true)
    }
  }

  def recycleAssets(assetIds: Set[String]): Unit = {

    assetIds.foreach { assetId =>
      txManager.withTransaction {
        val asset = getById(assetId)
        app.service.asset.setRecycledProp(asset, isRecycled = true)
        val recycledAsset = getById(assetId)
        app.service.stats.recycleAsset(recycledAsset)
        app.service.fileStore.recycleAsset(asset)
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
