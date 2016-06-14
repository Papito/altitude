package altitude.service

import java.awt.image.BufferedImage
import java.awt.{AlphaComposite, Graphics2D}
import java.io._
import javax.imageio.ImageIO

import altitude.exceptions.{FormatException, DuplicateException}
import altitude.models.search.{Query, QueryResult}
import altitude.models.{Stats, Asset, Folder, Preview}
import altitude.transactions.{AbstractTransactionManager, TransactionId}
import altitude.{Altitude, Const => C}
import net.codingwell.scalaguice.InjectorExtensions._
import org.imgscalr.Scalr
import org.slf4j.LoggerFactory
import play.api.libs.json.JsObject

class LibraryService(app: Altitude) {
  private final val log = LoggerFactory.getLogger(getClass)
  protected val txManager = app.injector.instance[AbstractTransactionManager]

  val PREVIEW_BOX_SIZE = app.config.getInt("preview.box.pixels")

  def add(obj: Asset)(implicit txId: TransactionId = new TransactionId): JsObject = {
    log.info(s"\nAdding asset with MD5: ${obj.md5}\n")
    val query = Query(Map(C("Asset.MD5") -> obj.md5))

    txManager.withTransaction[JsObject] {
      val existing = app.service.asset.query(query)

      if (existing.nonEmpty) {
        log.warn(s"Asset already exists for ${obj.path}")
        throw DuplicateException(obj.toJson, existing.records.head)
      }

      val assetJson: JsObject = app.service.asset.add(obj)
      val asset: Asset = assetJson

      // counters
      app.service.folder.incrAssetCount(obj.folderId)
      app.service.stats.incrementStat(Stats.TOTAL_ASSETS)
      // if there is no folder, increment the uncategorized counter
      if (Folder.UNCATEGORIZED.id.contains(obj.folderId)) {
        app.service.stats.incrementStat(Stats.UNCATEGORIZED_ASSETS)
      }

      // make and add preview, UNLESS we already have the data
      obj.previewData.length match {
        case 0 => addPreview(asset)
        case _ => {
          log.debug("Asset preview data already given")
          addPreviewData(asset, obj.previewData)
        }
      }

      assetJson
    }
  }

  def deleteById(id: String)(implicit txId: TransactionId = new TransactionId) = {
    txManager.withTransaction {
      val asset: Asset = getById(id)

      // of this asset is still uncategorized, update the stat
      if (Folder.UNCATEGORIZED.id.contains(asset.folderId)) {
        app.service.stats.decrementStat(Stats.UNCATEGORIZED_ASSETS)
      }
      app.service.stats.decrementStat(Stats.TOTAL_ASSETS)

      app.service.asset.deleteById(id)
    }
  }

  def getById(id: String)(implicit txId: TransactionId = new TransactionId): JsObject = {
    txManager.asReadOnly[JsObject] {
      app.service.asset.getById(id)
    }
  }

  def getPreview(asset_id: String): Preview = {
    app.service.preview.getById(asset_id)
  }

  def search(query: Query)(implicit txId: TransactionId = new TransactionId): QueryResult = {
    log.debug(s"Asset query $query")

    // parse out folder ids as a set
    val folderIds: Set[String] = query.params.getOrElse(C("Api.Folder.QUERY_ARG_NAME"), "")
      .toString.split(s"\\${C("Api.MULTI_VALUE_DELIM")}").map(_.trim).filter(_.nonEmpty).toSet

    log.debug(s"${folderIds.size} folder ids: $folderIds")

    txManager.asReadOnly[QueryResult] {
      val _query: Query = folderIds.isEmpty match {
        // no folder filtering, query as is
        case true => query
        // create a new query that includes folder children, since we are searching the hierarchy
        case false => {
          val allFolderIds = app.service.folder.flatChildrenIds(parentIds = folderIds)
          log.debug(s"Expanded folder ids: $allFolderIds")

          // repackage the query to include all folders (they will have to be re-parsed again)
          Query(
            params = query.params
              ++ Map(C("Api.Folder.QUERY_ARG_NAME") -> allFolderIds.mkString(C("Api.MULTI_VALUE_DELIM"))),
            page = query.page, rpp = query.rpp)
        }
      }

      log.info("SEARCH QUERY: " + _query)

      app.service.asset.query(_query)
    }
  }

  def genPreviewData(asset: Asset): Array[Byte] = {
    asset.mediaType.mediaType match {
      case "image" =>
        makeImageThumbnail(asset)
      case _ => new Array[Byte](0)
    }
  }

  private def addPreview(asset: Asset): Option[Preview] = {
    require(asset.id.nonEmpty)

    val previewData: Array[Byte] = genPreviewData(asset)

    previewData.length match {
      case size if size > 0 =>
        log.info(s"Saving preview for ${asset.path}")

        val preview: Preview = Preview(
          assetId=asset.id.get,
          mimeType=asset.mediaType.mime,
          data=previewData)

        app.service.preview.add(preview)

        Some(preview)
      case _ => None
    }
  }

  private def addPreviewData(asset: Asset, previewData: Array[Byte]): Preview = {
    require(asset.id.nonEmpty)

    val preview: Preview = Preview(
      assetId = asset.id.get,
      mimeType = asset.mediaType.mime,
      data = previewData)

    app.service.preview.add(preview)
    preview
  }

  private def makeImageThumbnail(asset: Asset): Array[Byte] = {
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

      val COMPOSITE_IMAGE: BufferedImage = new BufferedImage(PREVIEW_BOX_SIZE, PREVIEW_BOX_SIZE, BufferedImage.TYPE_INT_ARGB)
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

  private def updateAssetFolder(asset: Asset, folder: Folder)(implicit txId: TransactionId): Asset = {
    val updateObj: Asset = new Asset(
      id = asset.id,
      path = asset.path,
      md5 = asset.md5,
      mediaType = asset.mediaType,
      sizeBytes = asset.sizeBytes,
      folderId = folder.id.get,
      metadata = asset.metadata)

    app.service.asset.updateById(asset.id.get, updateObj, fields = List(C("Asset.FOLDER_ID")))
    app.service.folder.decrAssetCount(asset.folderId)
    app.service.folder.incrAssetCount(updateObj.folderId)
    updateObj
  }


  def moveAssetToFolder(assetId: String, folderId: String)(implicit txId: TransactionId = new TransactionId): Asset = {
    txManager.withTransaction[Asset] {
      val asset: Asset = this.getById(assetId)
      // this checks folder validity
      val folder: Folder = app.service.folder.getById(folderId)

      // if moving from uncategorized, decrement that stat
      if (Folder.UNCATEGORIZED.id.contains(asset.folderId)) {
        app.service.stats.decrementStat(Stats.UNCATEGORIZED_ASSETS)
      }

      updateAssetFolder(asset, folder)
     }
  }

  def moveAssetsToFolder(assetIds: Set[String], folderId: String)(implicit txId: TransactionId = new TransactionId): Unit = {
    txManager.withTransaction[Unit] {
      // this checks folder validity
      val folder: Folder = app.service.folder.getById(folderId)

      assetIds.foreach{  assetId: String =>
        val asset: Asset = this.getById(assetId)

        // if moving from uncategorized, decrement that stat
        if (Folder.UNCATEGORIZED.id.contains(asset.folderId)) {
          app.service.stats.decrementStat(Stats.UNCATEGORIZED_ASSETS)
        }

        updateAssetFolder(asset, folder)
      }
    }
  }

  def moveAssetToUncategorized(assetId: String)(implicit txId: TransactionId = new TransactionId) = {
    txManager.withTransaction {
      moveAssetToFolder(assetId, Folder.UNCATEGORIZED.id.get)
      app.service.stats.incrementStat(Stats.UNCATEGORIZED_ASSETS)
    }
  }

  def moveAssetsToUncategorized(assetIds: Set[String])(implicit txId: TransactionId = new TransactionId) = {
    txManager.withTransaction[Unit] {
      app.service.stats.decrementStat(Stats.UNCATEGORIZED_ASSETS, assetIds.size)
      moveAssetsToFolder(assetIds, Folder.UNCATEGORIZED.id.get)
    }
  }

  def recycleAsset(assetId: String)(implicit txId: TransactionId = new TransactionId) = {
    txManager.withTransaction {
      val asset: Asset = this.getById(assetId)
      app.service.trash.recycleAsset(assetId)
      app.service.stats.incrementStat(Stats.RECYCLED_ASSETS)
      app.service.folder.decrAssetCount(asset.folderId)
    }
  }

  def recycleAssets(assetIds: Set[String])(implicit txId: TransactionId = new TransactionId): Unit = {
    txManager.withTransaction[Unit] {
      assetIds.foreach(recycleAsset)
    }
  }
}
