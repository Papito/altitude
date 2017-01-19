package altitude.service

import java.awt.image.BufferedImage
import java.awt.{AlphaComposite, Graphics2D}
import java.io._
import javax.imageio.ImageIO


import altitude.exceptions.{DuplicateException, FormatException, IllegalOperationException}
import altitude.models.{Stats, Preview, Data, Asset}
import altitude.transactions.{AbstractTransactionManager, TransactionId}
import altitude.util.{QueryResult, Query}
import altitude.{Altitude, Const => C, Context}
import net.codingwell.scalaguice.InjectorExtensions._
import org.imgscalr.Scalr
import org.slf4j.LoggerFactory
import play.api.libs.json.{JsObject, Json}

class LibraryService(app: Altitude) {
  private final val log = LoggerFactory.getLogger(getClass)
  protected val txManager = app.injector.instance[AbstractTransactionManager]

  val PREVIEW_BOX_SIZE = app.config.getInt("preview.box.pixels")

  def add(obj: Asset)(implicit ctx: Context, txId: TransactionId = new TransactionId): JsObject = {
    log.info(s"\nAdding asset with MD5: ${obj.md5}\n")

    if (app.service.folder.isRootFolder(obj.folderId)) {
      throw new IllegalOperationException("Cannot have assets in root folder")
    }

    val query = Query(Map(C.Asset.MD5 -> obj.md5))

    txManager.withTransaction[JsObject] {
      val existing = app.service.asset.query(query)

      if (existing.nonEmpty) {
        log.warn(s"Asset already exists for ${obj.path}")
        throw DuplicateException(obj.toJson, existing.records.head)
      }

      val data = app.service.library.genPreviewData(obj)

      /**
      * Process metadata and append it to the asset
      */
      val metadata = app.service.metadata.cleanAndValidateMetadata(obj.metadata)

      // mush the metadata JSON key into the JSON repr of the asset and get a new asset
      val assetToAdd: Asset = Asset(
        id = obj.id,
        userId = obj.userId,
        assetType = obj.assetType,
        path = obj.path,
        md5 = obj.md5,
        sizeBytes = obj.sizeBytes,
        folderId = obj.folderId,
        metadata = metadata,
        extractedMetadata = obj.extractedMetadata,
        previewData = data)

      val asset: Asset = app.service.asset.add(assetToAdd)

      /**
      * Search index
      */
      //app.service.search.indexAsset(asset)

      /**
      * Update repository counters
      */
      app.service.folder.incrAssetCount(obj.folderId)
      app.service.stats.incrementStat(Stats.TOTAL_ASSETS)
      app.service.stats.incrementStat(Stats.TOTAL_BYTES, asset.sizeBytes)

      // if there is no folder, increment the uncategorized counter
      if (ctx.repo.uncatFolderId == obj.folderId) {
        app.service.stats.incrementStat(Stats.UNCATEGORIZED_ASSETS)
      }

      // make and add preview, UNLESS we already have the data
      obj.previewData.length match {
        case 0 => addPreview(asset)
        case _ =>
          log.debug("Asset preview data already given")
          addPreviewData(asset, obj.previewData)
      }

      asset
    }
  }

  def deleteById(id: String)(implicit ctx: Context, txId: TransactionId = new TransactionId) = {
    txManager.withTransaction {
      val asset: Asset = getById(id)

      // of this asset is still uncategorized, update the stat
      if (ctx.repo.uncatFolderId == asset.folderId) {
        app.service.stats.decrementStat(Stats.UNCATEGORIZED_ASSETS)
      }
      app.service.stats.decrementStat(Stats.TOTAL_ASSETS)
      app.service.stats.decrementStat(Stats.TOTAL_BYTES, asset.sizeBytes)

      app.service.asset.deleteById(id)
    }
  }

  def getById(id: String)(implicit ctx: Context, txId: TransactionId = new TransactionId): JsObject = {
    txManager.asReadOnly[JsObject] {
      app.service.asset.getById(id)
    }
  }

  def getPreview(assetId: String)(implicit ctx: Context, txId: TransactionId = new TransactionId): Preview = {
    app.service.preview.getById(assetId)
  }

  def getData(assetId: String)(implicit ctx: Context, txId: TransactionId = new TransactionId): Data = {
    app.service.data.getById(assetId)
  }

  def query(query: Query)(implicit ctx: Context, txId: TransactionId = new TransactionId): QueryResult = {
    log.debug(s"Asset query $query")

    // parse out folder ids as a set
    val folderIds: Set[String] = query.params.getOrElse(C.Api.Folder.QUERY_ARG_NAME, "")
      .toString.split(s"\\${C.Api.MULTI_VALUE_DELIM}").map(_.trim).filter(_.nonEmpty).toSet

    log.debug(s"${folderIds.size} folder ids: $folderIds")

    txManager.asReadOnly[QueryResult] {
      val _query: Query = folderIds.isEmpty match {
        // no folder filtering, query as is
        case true => query
        // create a new query that includes folder children, since we are searching the hierarchy
        case false =>
          val allFolderIds = app.service.folder.flatChildrenIds(parentIds = folderIds)
          log.debug(s"Expanded folder ids: $allFolderIds")

          // repackage the query to include all folders (they will have to be re-parsed again)
          Query(
            params = query.params
              ++ Map(C.Api.Folder.QUERY_ARG_NAME -> allFolderIds.mkString(C.Api.MULTI_VALUE_DELIM)),
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
        log.info(s"Saving preview for ${asset.path}")

        val preview: Preview = Preview(
          assetId=asset.id.get,
          mimeType=asset.assetType.mime,
          data=previewData)

        app.service.preview.add(preview)

        Some(preview)
      case _ => None
    }
  }

  private def addPreviewData(asset: Asset, previewData: Array[Byte])
                            (implicit ctx: Context, txId: TransactionId): Preview = {
    require(asset.id.nonEmpty)

    val preview: Preview = Preview(
      assetId = asset.id.get,
      mimeType = asset.assetType.mime,
      data = previewData)

    app.service.preview.add(preview)
    preview
  }

  private def makeImageThumbnail(asset: Asset)
                                (implicit ctx: Context, txId: TransactionId): Array[Byte] = {
    try {
      val inFile = new File(asset.path)
      val srcImage: BufferedImage = ImageIO.read(inFile)
      val scaledImage: BufferedImage = Scalr.resize(srcImage, Scalr.Method.ULTRA_QUALITY, PREVIEW_BOX_SIZE)
      val height: Int = scaledImage.getHeight
      val width: Int = scaledImage.getWidth

      val x: Int = height > width match {
        case true => (PREVIEW_BOX_SIZE - width) / 2
        case false => 0
      }

      val y: Int = height < width match {
        case true => (PREVIEW_BOX_SIZE - height) / 2
        case false => 0
      }

      val COMPOSITE_IMAGE: BufferedImage =
        new BufferedImage(PREVIEW_BOX_SIZE, PREVIEW_BOX_SIZE, BufferedImage.TYPE_INT_ARGB)
      val G2D: Graphics2D = COMPOSITE_IMAGE.createGraphics

      G2D.setComposite(AlphaComposite.Clear)
      G2D.fillRect(0, 0, PREVIEW_BOX_SIZE, PREVIEW_BOX_SIZE)
      G2D.setComposite(AlphaComposite.Src)
      G2D.drawImage(scaledImage, x, y, null)
      val byteArray: ByteArrayOutputStream = new ByteArrayOutputStream
      ImageIO.write(COMPOSITE_IMAGE, "png", byteArray)

      byteArray.toByteArray
    } catch {
      case ex: Exception => {
        log.error(s"Error generating preview for $asset")
        altitude.Util.logStacktrace(ex)
        throw FormatException(asset)
      }
    }
  }

  def moveAssetToFolder(assetId: String, folderId: String)
                       (implicit ctx: Context, txId: TransactionId = new TransactionId): Asset = {
    txManager.withTransaction[Asset] {
      moveAssetsToFolder(Set(assetId), folderId)
      getById(assetId)
    }
  }

  def moveAssetsToFolder(assetIds: Set[String], folderId: String)
                        (implicit ctx: Context, txId: TransactionId = new TransactionId) = {
    txManager.withTransaction {
      assetIds.foreach {assetId =>

        // cannot have assets in root folder - just other folders
        if (app.service.folder.isRootFolder(folderId)) {
          throw new IllegalOperationException("Cannot move assets to root folder")
        }

        // this checks folder validity
        app.service.folder.getById(folderId)

        val asset: Asset = this.getById(assetId)

        // if moving from uncategorized, decrement that stat
        if (ctx.repo.uncatFolderId == asset.folderId) {
          app.service.stats.decrementStat(Stats.UNCATEGORIZED_ASSETS)
        }

        // point asset to the new folder
        val updatedAsset: Asset = asset ++ Json.obj(C.Asset.FOLDER_ID -> folderId)

        val i = app.service.asset.updateById(asset.id.get, updatedAsset, fields = List(C.Asset.FOLDER_ID))
        app.service.folder.decrAssetCount(asset.folderId)
      }

      app.service.folder.incrAssetCount(folderId, assetIds.size)
    }
  }

  def moveAssetToUncategorized(assetId: String)
                              (implicit ctx: Context, txId: TransactionId = new TransactionId) = {
    txManager.withTransaction {
      moveAssetsToUncategorized(Set(assetId))
    }
  }

  def moveAssetsToUncategorized(assetIds: Set[String])
                               (implicit ctx: Context, txId: TransactionId = new TransactionId) = {
    txManager.withTransaction {
      moveAssetsToFolder(assetIds, ctx.repo.uncatFolderId)
      app.service.stats.incrementStat(Stats.UNCATEGORIZED_ASSETS, assetIds.size)
    }
  }


  def recycleAsset(assetId: String)
                  (implicit ctx: Context, txId: TransactionId = new TransactionId) = {
    txManager.withTransaction {
      recycleAssets(Set(assetId))
      getById(assetId)
    }
  }

  def recycleAssets(assetIds: Set[String])
                   (implicit ctx: Context, txId: TransactionId = new TransactionId) = {
    var totalBytes = 0L

    txManager.withTransaction {
      assetIds.foreach { assetId =>
        val asset: Asset = getById(assetId)
        app.service.asset.setAssetAsRecycled(assetId, isRecycled = true)

        app.service.folder.decrAssetCount(asset.folderId)

        if (app.service.folder.getUncatFolder.id.contains(asset.folderId)) {
          app.service.stats.decrementStat(Stats.UNCATEGORIZED_ASSETS)
        }

        totalBytes += asset.sizeBytes
      }

      app.service.stats.decrementStat(Stats.TOTAL_ASSETS, assetIds.size)
      app.service.stats.incrementStat(Stats.RECYCLED_ASSETS, assetIds.size)
      app.service.stats.decrementStat(Stats.TOTAL_BYTES, totalBytes)
      app.service.stats.incrementStat(Stats.RECYCLED_BYTES, totalBytes)
    }
  }

  def moveRecycledAssetToFolder(assetId: String, folderId: String)
                               (implicit ctx: Context, txId: TransactionId = new TransactionId): Asset = {
    txManager.withTransaction[Asset] {
      moveRecycledAssetsToFolder(Set(assetId), folderId)
      getById(assetId)
    }
  }

  def moveRecycledAssetsToFolder(assetIds: Set[String], folderId: String)
                                (implicit ctx: Context, txId: TransactionId = new TransactionId) = {
    var totalBytes = 0L

    txManager.withTransaction {
      val restoredAssetIds: Set[String] = assetIds.map { assetId: String =>
        app.service.asset.setAssetAsRecycled(assetId, isRecycled = false)
        val asset: Asset = getById(assetId)

        totalBytes += asset.sizeBytes
        asset.id.get
      }

      moveAssetsToFolder(restoredAssetIds, folderId)
      app.service.stats.incrementStat(Stats.TOTAL_ASSETS, assetIds.size)
      app.service.stats.decrementStat(Stats.RECYCLED_ASSETS, assetIds.size)
      app.service.stats.incrementStat(Stats.TOTAL_BYTES, totalBytes)
      app.service.stats.decrementStat(Stats.RECYCLED_BYTES, totalBytes)
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
                           (implicit ctx: Context, txId: TransactionId = new TransactionId) = {
    var totalBytes = 0L

    txManager.withTransaction {
      assetIds.foreach { assetId =>
        app.service.asset.setAssetAsRecycled(assetId, isRecycled = false)
        val asset: Asset = getById(assetId)
        totalBytes += asset.sizeBytes
      }

      app.service.stats.incrementStat(Stats.TOTAL_ASSETS, assetIds.size)
      app.service.stats.decrementStat(Stats.RECYCLED_ASSETS, assetIds.size)
      app.service.stats.incrementStat(Stats.TOTAL_BYTES, totalBytes)
      app.service.stats.decrementStat(Stats.RECYCLED_BYTES, totalBytes)
    }
  }
}
