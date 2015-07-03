package altitude.service

import java.awt.image.BufferedImage
import java.awt.{Color, Graphics2D}
import java.io._
import javax.imageio.ImageIO

import altitude.dao.LibraryDao
import altitude.exceptions.DuplicateException
import altitude.models.Asset
import altitude.models.search.Query
import altitude.transactions.TransactionId
import altitude.{Altitude, Const => C}
import net.codingwell.scalaguice.InjectorExtensions._
import org.imgscalr.Scalr
import org.slf4j.LoggerFactory
import play.api.libs.json.JsObject

class LibraryService(app: Altitude) extends BaseService[Asset](app) {
  val log =  LoggerFactory.getLogger(getClass)
  override protected val DAO = app.injector.instance[LibraryDao]
  val PREVIEW_BOX_SIZE = 200 //FIXME: to settings

  override def add(obj: Asset)(implicit txId: TransactionId = new TransactionId): JsObject = {
    throw new NotImplementedError
  }

  def addAsset(asset: Asset)(implicit txId: TransactionId = new TransactionId): Asset = {
    txManager.withTransaction[JsObject] {
      val query = Query(Map(C.Asset.MD5 -> asset.md5))
      val existing = DAO.query(query)

      if (existing.nonEmpty) {
        log.warn(s"Asset already exists for ${asset.path}")
        throw new DuplicateException(s"Duplicate for ${asset.path}")
      }

      DAO.add(asset)
      addImagePreview(asset)
    }
  }

  private def addImagePreview(asset: Asset): Asset = {
    asset.mediaType.mediaType match {
      case "image" =>
        val imageData = makeImageThumbnail(asset)
        log.debug(s"Asset image size ${imageData.length}")

        DAO.addImagePreview(asset, imageData)

      // Return as is
      case _ => asset
    }
  }

  private def makeImageThumbnail(asset: Asset): Array[Byte] = {
    //FIXME: use Option and try/catch
    val inFile = new File(asset.path)
    val srcImage: BufferedImage = ImageIO.read(inFile)
    val scaledImage: BufferedImage = Scalr.resize(srcImage, Scalr.Method.QUALITY, PREVIEW_BOX_SIZE)
    val large: BufferedImage = new BufferedImage(PREVIEW_BOX_SIZE, PREVIEW_BOX_SIZE, BufferedImage.TYPE_INT_RGB)
    val g: Graphics2D = large.createGraphics
    g.setBackground(Color.WHITE)
    g.fillRect(0, 0, PREVIEW_BOX_SIZE, PREVIEW_BOX_SIZE)
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
    g.drawImage(large, 0, 0, null)
    g.drawImage(scaledImage, x, y, null)
    g.dispose()
    val byteArray: ByteArrayOutputStream = new ByteArrayOutputStream
    ImageIO.write(large, "jpg", byteArray)
    byteArray.toByteArray
  }
}
