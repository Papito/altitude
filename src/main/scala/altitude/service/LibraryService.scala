package altitude.service

import java.awt.image.BufferedImage
import java.awt.{AlphaComposite, Graphics2D}
import java.io._
import javax.imageio.ImageIO

import altitude.exceptions.DuplicateException
import altitude.models.search.Query
import altitude.models.{Asset, Preview}
import altitude.transactions.{AbstractTransactionManager, TransactionId}
import altitude.{Altitude, Const => C}
import net.codingwell.scalaguice.InjectorExtensions._
import org.imgscalr.Scalr
import org.slf4j.LoggerFactory
import play.api.libs.json.JsObject

class LibraryService(app: Altitude) {
  val log =  LoggerFactory.getLogger(getClass)
  protected val txManager = app.injector.instance[AbstractTransactionManager]

  val PREVIEW_BOX_SIZE = app.config.getInt("preview.box.pixels")
  val COMPOSITE_IMAGE: BufferedImage = new BufferedImage(PREVIEW_BOX_SIZE, PREVIEW_BOX_SIZE, BufferedImage.TYPE_INT_ARGB)
  val G2D: Graphics2D = COMPOSITE_IMAGE.createGraphics

  def add(obj: Asset)(implicit txId: TransactionId = new TransactionId): JsObject = {
    txManager.withTransaction[JsObject] {
      log.info(s"\nAdding asset with MD5: ${obj.md5}\n")
      val query = Query(Map(C.Asset.MD5 -> obj.md5))
      val existing = app.service.asset.query(query)

      if (existing.nonEmpty) {
        log.warn(s"Asset already exists for ${obj.path}")
        throw new DuplicateException(s"Duplicate for ${obj.path}")
      }

      val assetJson: JsObject = app.service.asset.add(obj)
      addPreview(assetJson)
      assetJson
    }
  }

  def getById(id: String)(implicit txId: TransactionId = new TransactionId): JsObject = {
    app.service.asset.getById(id)
  }


  def getPreview(asset_id: String)(implicit txId: TransactionId = new TransactionId): Preview = {
    log.debug(s"Getting preview for '$asset_id'")
    app.service.preview.getById(asset_id)
  }

  private def addPreview(asset: Asset)(implicit txId: TransactionId = new TransactionId): Option[Preview] = {
    require(asset.id.nonEmpty)
    val previewData: Array[Byte] = asset.mediaType.mediaType match {
      case "image" =>
        makeImageThumbnail(asset)
      case _ => new Array[Byte](0)
    }

    previewData.length match {
      //FIXME: How to make this > 0 condition?
      case 0 => None
      case _ =>
        log.info(s"Saving preview for ${asset.path}")

        val preview: Preview = Preview(
          asset_id=asset.id.get,
          mime_type=asset.mediaType.mime,
          data=previewData)

        app.service.preview.add(preview)

        Some(preview)
    }
  }

  private def makeImageThumbnail(asset: Asset): Array[Byte] = {

    val inFile = new File(asset.path)
    val srcImage: BufferedImage = ImageIO.read(inFile)
    val scaledImage: BufferedImage = Scalr.resize(srcImage, Scalr.Method.QUALITY, PREVIEW_BOX_SIZE)
    var x: Int = 0
    var y: Int = 0
    val height: Int = scaledImage.getHeight
    val width: Int = scaledImage.getWidth
    if (height > width) {
      x = (PREVIEW_BOX_SIZE - width) / 2
    }
    if (height < width) {
      y = (PREVIEW_BOX_SIZE - height) / 2
    }

    G2D.setComposite(AlphaComposite.Clear)
    G2D.fillRect(0, 0, PREVIEW_BOX_SIZE, PREVIEW_BOX_SIZE)
    G2D.drawImage(COMPOSITE_IMAGE, 0, 0, null)
    G2D.setComposite(AlphaComposite.Src)
    G2D.drawImage(scaledImage, x, y, null)
    val byteArray: ByteArrayOutputStream = new ByteArrayOutputStream
    ImageIO.write(COMPOSITE_IMAGE, "png", byteArray)

    byteArray.toByteArray
  }
}
