package software.altitude.core.service

import java.awt.image.BufferedImage
import java.awt.{AlphaComposite, Graphics2D}
import java.io._
import javax.imageio.ImageIO

import net.codingwell.scalaguice.InjectorExtensions._
import org.imgscalr.Scalr
import org.slf4j.LoggerFactory
import play.api.libs.json.{JsObject, Json}
import software.altitude.core.Const.Api
import software.altitude.core.Const.Api.Folder
import software.altitude.core.models._
import software.altitude.core.transactions.{AbstractTransactionManager, TransactionId}
import software.altitude.core.util.{Query, QueryResult}
import software.altitude.core.{Altitude, Const => C, Context, _}

class LibraryService(val app: Altitude) {
  private final val log = LoggerFactory.getLogger(getClass)
  protected val txManager: AbstractTransactionManager = app.injector.instance[AbstractTransactionManager]

  // FIXME: does not belong here - image specific
  val previewBoxSize: Int = app.config.getInt("preview.box.pixels")

  def add(assetIn: Asset)(implicit ctx: Context, txId: TransactionId = new TransactionId): JsObject = {
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

      val assetId = BaseModel.genId

      val fileName = app.service.fileStore.calculateNextAvailableFilename(assetIn)

      val assetToAdd: Asset = Asset(
        id = Some(assetId),
        data = assetIn.data,
        userId = assetIn.userId,
        assetType = assetIn.assetType,
        fileName = fileName,
        checksum = assetIn.checksum,
        sizeBytes = assetIn.sizeBytes,
        folderId = assetIn.folderId,
        metadata = metadata,
        extractedMetadata = assetIn.extractedMetadata)

      log.info(s"Adding asset: $assetToAdd")

      val storedAsset: Asset = app.service.asset.add(assetToAdd)

      /**
      * Add to search index
      */
      app.service.search.indexAsset(storedAsset)

      /**
      * Update repository counters
      */
      app.service.stats.addAsset(assetToAdd)

      // add preview data
      addPreview(assetToAdd)

      app.service.fileStore.addAsset(assetToAdd)

      val path = app.service.fileStore.getAssetPath(assetToAdd)

      storedAsset ++ Json.obj(C.Asset.PATH -> path)
    }
  }

  def deleteById(id: String)(implicit ctx: Context, txId: TransactionId = new TransactionId): Unit = {
    throw new NotImplementedError
  }

  def getById(id: String)(implicit ctx: Context, txId: TransactionId = new TransactionId): JsObject = {
    txManager.asReadOnly[JsObject] {
      val asset: Asset = app.service.asset.getById(id)
      val path = app.service.fileStore.getAssetPath(asset)
      asset.toJson ++ Json.obj(C.Asset.PATH -> path)
    }
  }

  def getByChecksum(checksum: String)(implicit ctx: Context, txId: TransactionId = new TransactionId): Option[Asset] = {
    txManager.asReadOnly[Option[Asset]] {
      val query = Query(Map(C.Asset.CHECKSUM -> checksum))
      val existing = app.service.asset.query(query)
      if (existing.nonEmpty) Some(existing.records.head: Asset) else None
    }
  }

  def getPreview(assetId: String)(implicit ctx: Context, txId: TransactionId = new TransactionId): Preview = {
    app.service.fileStore.getPreviewById(assetId)
  }

  def getData(assetId: String)(implicit ctx: Context, txId: TransactionId = new TransactionId): Data = {
    app.service.fileStore.getById(assetId)
  }

  def query(query: Query)(implicit ctx: Context, txId: TransactionId = new TransactionId): QueryResult = {
    log.debug(s"Asset query $query")

    // parse out folder ids as a set
    val folderIds: Set[String] = query.params.getOrElse(Folder.QUERY_ARG_NAME, "")
      .toString.split(s"\\${Api.MULTI_VALUE_DELIM}").map(_.trim).filter(_.nonEmpty).toSet

    log.debug(s"${folderIds.size} folder ids: $folderIds")

    txManager.asReadOnly[QueryResult] {
      val _query: Query = if (folderIds.isEmpty) {
        // no folder filtering, query as is
        query
      }
      else {
        // create a new query that includes folder children, since we are searching the hierarchy
        val allFolderIds = app.service.folder.flatChildrenIds(parentIds = folderIds)
        log.debug(s"Expanded folder ids: $allFolderIds")

        // repackage the query to include all folders (they will have to be re-parsed again)
        Query(
          params = query.params
            ++ Map(Api.Folder.QUERY_ARG_NAME -> allFolderIds.mkString(C.Api.MULTI_VALUE_DELIM)),
          page = query.page, rpp = query.rpp)

      }

      log.info("SEARCH QUERY: " + _query)

      app.service.asset.query(_query)
    }
  }

  def queryRecycled(query: Query)(implicit ctx: Context, txId: TransactionId = new TransactionId): QueryResult = {
    app.service.asset.queryRecycled(query)
  }

  def genPreviewData(asset: Asset)
                    (implicit ctx: Context, txId: TransactionId = new TransactionId): Array[Byte] = {
    asset.assetType.mediaType match {
      case "image" =>
        makeImageThumbnail(asset)
      case _ => new Array[Byte](0)
    }
  }

  private def addPreview(asset: Asset)(implicit ctx: Context, txId: TransactionId): Option[Preview] = {
    require(asset.id.nonEmpty)

    val previewData: Array[Byte] = genPreviewData(asset)

    previewData.length match {
      case size if size > 0 =>

        val preview: Preview = Preview(
          assetId=asset.id.get,
          mimeType=asset.assetType.mime,
          data=previewData)

        app.service.fileStore.addPreview(preview)

        Some(preview)
      case _ => None
    }
  }

  private def makeImageThumbnail(asset: Asset)
                                (implicit ctx: Context, txId: TransactionId): Array[Byte] = {
    try {
      require(asset.data.length != 0)
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

  def moveAssetToFolder(assetId: String, folderId: String)
                       (implicit ctx: Context, txId: TransactionId = new TransactionId): Asset = {
    txManager.withTransaction[Asset] {
      moveAssetsToFolder(Set(assetId), folderId)
      getById(assetId)
    }
  }

  def moveAssetToTriage(assetId: String)
                              (implicit ctx: Context, txId: TransactionId = new TransactionId): Unit = {
    txManager.withTransaction {
      moveAssetsToTriage(Set(assetId))
    }
  }

  def moveAssetsToTriage(assetIds: Set[String])
                               (implicit ctx: Context, txId: TransactionId = new TransactionId): Unit = {
    txManager.withTransaction {
      moveAssetsToFolder(assetIds, ctx.repo.triageFolderId)
    }
  }


  def recycleAsset(assetId: String)
                  (implicit ctx: Context, txId: TransactionId = new TransactionId): Asset = {
    txManager.withTransaction {
      recycleAssets(Set(assetId))
      getById(assetId)
    }
  }

  def recycleAssets(assetIds: Set[String])
                   (implicit ctx: Context, txId: TransactionId = new TransactionId): Unit = {

      assetIds.foreach { assetId =>
        txManager.withTransaction {
          val asset = getById(assetId)
          app.service.asset.setAssetAsRecycled(assetId, isRecycled = true)
          val recycledAsset = getById(assetId)
          app.service.stats.recycleAsset(recycledAsset)
          app.service.fileStore.recycleAsset(asset)
        }
      }
  }

  /**
   * Delete a folder by ID, including its children. Does not allow deleting the root folder,
   * or any system folders.
   *
   * Recycle all the assets in the tree.
   */
  def deleteFolderById(id: String)
                  (implicit ctx: Context, txId: TransactionId = new TransactionId): Unit = {
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
        app.service.folder.flatChildrenIdsWithDepths(id, app.service.folder.repositoryFolders()).sortBy(_._1).reverse
                                                        /* ^^^ sort by depth */

      // now delete the children, most-deep first
      childrenAndDepths.foreach {t =>
        val folderId = t._2
        // set all the assets as recycled
        val folderAssetsQuery = Query(
          params = Map(Api.Folder.QUERY_ARG_NAME -> folderId))

        val results: QueryResult = app.service.library.query(folderAssetsQuery)

        results.records.foreach { record =>
          val assetId = (record \ C.Asset.ID).as[String]
          this.recycleAsset(assetId)
        }
        app.service.folder.deleteById(folderId)}

      app.service.fileStore.deleteFolder(folder)
    }
 }

  def moveAssetsToFolder(assetIds: Set[String], destFolderId: String)
                        (implicit ctx: Context, txId: TransactionId = new TransactionId): Unit = {

    def move(asset: Asset): Unit = {
      // cannot move to the same folder
      if (!asset.isRecycled && asset.folderId == destFolderId) {
        return
      }

      app.service.stats.moveAsset(asset, destFolderId)

      // if it's a recycled asset, we are adding it back to the general population
      if (asset.isRecycled) {
        app.service.asset.setAssetAsRecycled(assetId = asset.id.get, isRecycled = false)
      }

      // point asset to the new folder
      val updatedAsset: Asset = asset ++ Json.obj(
        C.Asset.FOLDER_ID -> destFolderId,
        C.Asset.IS_RECYCLED -> false)

      app.service.asset.updateById(
        asset.id.get, updatedAsset,
        fields = List(C.Asset.FOLDER_ID))

      app.service.fileStore.moveAsset(asset, updatedAsset)
    }

    txManager.withTransaction {
      // ensure the folder exists
      app.service.folder.getById(destFolderId)

      assetIds.foreach {assetId =>
        // cannot have assets in root folder - just other folders
        if (app.service.folder.isRootFolder(destFolderId)) {
          throw IllegalOperationException("Cannot move assets to root folder")
        }

        val asset: Asset = getById(assetId)

        move(asset)
      }
    }
  }

  def renameAsset(assetId: String, newFilename: String)
                 (implicit ctx: Context, txId: TransactionId = new TransactionId): Asset = {

    txManager.withTransaction[Asset] {
      val asset: Asset = getById(assetId)

      if (asset.isRecycled) {
        throw IllegalOperationException(s"Cannot rename a recycled asset: [$asset]")
      }

      val updatedAsset: Asset = asset ++ Json.obj(
          C.Asset.FILENAME -> newFilename,
          C.Asset.PATH -> app.service.fileStore.getPathWithNewFilename(asset, newFilename))

      app.service.asset.updateById(
        asset.id.get, updatedAsset,
        fields = List(C.Asset.FILENAME))

      app.service.fileStore.moveAsset(asset, updatedAsset)

      updatedAsset
    }
  }

  def renameFolder(folderId: String, newName: String)
                  (implicit ctx: Context, txId: TransactionId = new TransactionId): Folder = {
    txManager.withTransaction[Folder] {
      app.service.folder.rename(folderId, newName)
    }
  }

  def restoreRecycledAsset(assetId: String)
                          (implicit ctx: Context, txId: TransactionId = new TransactionId): Asset = {
    txManager.withTransaction[Asset] {
      restoreRecycledAssets(Set(assetId))
      getById(assetId)
    }
  }

  def restoreRecycledAssets(assetIds: Set[String])
                           (implicit ctx: Context, txId: TransactionId = new TransactionId): Unit = {
    log.info(s"Restoring recycled assets [${assetIds.mkString(",")}]")

    assetIds.foreach { assetId =>
      log.info(s"Restoring recycled asset [$assetId]")

      val asset: Asset = getById(assetId)
      val existing = getByChecksum(asset.checksum)

      if (existing.isDefined) {
        throw DuplicateException(existingAssetId = existing.get.id.get)
      }

      txManager.withTransaction {
        app.service.asset.setAssetAsRecycled(assetId, isRecycled = false)
        val restoredAsset: Asset = getById(assetId)
        app.service.stats.restoreAsset(restoredAsset)
        app.service.fileStore.restoreAsset(restoredAsset)
      }
    }
  }
}
