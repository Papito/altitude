package altitude.service

import java.awt.image.BufferedImage
import java.awt.{AlphaComposite, Graphics2D}
import java.io._
import javax.imageio.ImageIO

import altitude.exceptions.DuplicateException
import altitude.models.search.Query
import altitude.models.{Asset, Folder, Preview}
import altitude.transactions.{AbstractTransactionManager, TransactionId}
import altitude.{Altitude, Const => C}
import net.codingwell.scalaguice.InjectorExtensions._
import org.imgscalr.Scalr
import org.slf4j.LoggerFactory
import play.api.libs.json.JsObject

class LibraryService(app: Altitude) {
  private final val log = LoggerFactory.getLogger(getClass)
  protected val txManager = app.injector.instance[AbstractTransactionManager]

  val PREVIEW_BOX_SIZE = app.config.getInt("result.box.pixels")

  def add(obj: Asset)(implicit txId: TransactionId = new TransactionId): JsObject = {
    log.info(s"\nAdding asset with MD5: ${obj.md5}\n")
    val query = Query(Map(C.Asset.MD5 -> obj.md5))

    txManager.withTransaction[JsObject] {
      val existing = app.service.asset.query(query)

      if (existing.nonEmpty) {
        log.warn(s"Asset already exists for ${obj.path}")
        throw DuplicateException(obj.toJson)
      }

      val assetJson: JsObject = app.service.asset.add(obj)
      app.service.folder.incrAssetCount(obj.folderId)

      addPreview(assetJson)
      assetJson
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

  def search(query: Query)(implicit txId: TransactionId = new TransactionId): List[Asset] = {
    log.debug(s"Asset query $query")

    // parse out folder ids as a set
    val folderIds: Set[String] = query.params.getOrElse(C.Api.Folder.QUERY_ARG_NAME, "")
      .toString.split(s"\\${C.Api.MULTI_VALUE_DELIM}").map(_.trim).filter(_.nonEmpty).toSet

    log.debug(s"${folderIds.size} folder ids: $folderIds")

    txManager.asReadOnly[List[Asset]] {
      val _query: Query = folderIds.isEmpty match {
        case false => {
          val allFolders = app.service.folder.getAll
          val allFolderIds = app.service.folder.flatChildrenIds(parentIds = folderIds, all = allFolders)
          log.debug(s"Expanded folder ids: $allFolderIds")

          // repackage the query to include all folders (they will have to reparsed again)
          Query(
            params = query.params
              ++ Map(C.Api.Folder.QUERY_ARG_NAME -> allFolderIds.mkString(C.Api.MULTI_VALUE_DELIM)),
            page = query.page, rpp = query.rpp)
        }
        case true => query
      }

      log.info("SEARCH QUERY: " + _query)

      val searchResultData: List[JsObject] = app.service.asset.query(_query)
      for (data <- searchResultData) yield Asset.fromJson(data)
    }
  }

  private def addPreview(asset: Asset): Option[Preview] = {
    require(asset.id.nonEmpty)

    val previewData: Array[Byte] = asset.mediaType.mediaType match {
      case "image" =>
        makeImageThumbnail(asset)
      case _ => new Array[Byte](0)
    }

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

  private def makeImageThumbnail(asset: Asset): Array[Byte] = {
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
  }

  def moveToFolder(assetId: String, folderId: String)(implicit txId: TransactionId = new TransactionId): Asset = {
    txManager.withTransaction[Asset] {
      val asset: Asset = this.getById(assetId)
      // we can go straight to DB, but we need to do a check on folder validity
      val folder: Folder = app.service.folder.getById(folderId)

      val updateObj: Asset = new Asset(
        id = asset.id,
        path = asset.path,
        md5 = asset.md5,
        mediaType = asset.mediaType,
        sizeBytes = asset.sizeBytes,
        folderId = folder.id.get,
        metadata = asset.metadata)

      app.service.asset.updateById(assetId, updateObj, fields = List(C.Asset.FOLDER_ID))
      app.service.folder.decrAssetCount(asset.folderId)
      app.service.folder.incrAssetCount(updateObj.folderId)
      updateObj
    }
  }

  def moveToUncategorized(assetId: String)(implicit txId: TransactionId = new TransactionId) = {
    txManager.withTransaction {
      moveToFolder(assetId, Folder.UNCATEGORIZED.id.get)
    }
  }

  def moveToTrash(assetId: String)(implicit txId: TransactionId = new TransactionId) = {
    txManager.withTransaction {
      moveToFolder(assetId, Folder.TRASH.id.get)
    }
  }
}
