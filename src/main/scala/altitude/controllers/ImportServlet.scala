package altitude.controllers

import altitude.exceptions.{EndOfImportAssets, DuplicateException}
import altitude.models.{Asset, FileImportAsset}
import org.json4s._
import org.scalatra._
import org.scalatra.atmosphere._
import org.scalatra.json.{JValueResult, JacksonJsonSupport}
import org.slf4j.LoggerFactory
import play.api.libs.json.{JsNumber, JsObject, JsString}
import scala.concurrent.ExecutionContext.Implicits.global

class ImportServlet extends BaseController  with JValueResult
with JacksonJsonSupport with SessionSupport with AtmosphereSupport  {
  val log = LoggerFactory.getLogger(getClass)
  implicit protected val jsonFormats: Formats = DefaultFormats

  get("/") {
    contentType="text/html"
    ssp("/import")
  }

  atmosphere("/ws") {
    val assets = app.service.fileImport.getFilesToImport(path="/mnt/hgfs/import")
    val assetsIt = assets.toIterator
    var criticalException: Option[Throwable] = None

    new AtmosphereClient {
      def receive: AtmoReceive = {
        case Connected =>
          log.info("Client connected")
        case Disconnected(disconnector, Some(error)) =>
          log.info("Client disconnected")
        case Error(Some(error)) =>
          error.printStackTrace()

        case TextMessage(text) =>
          log.info(s"Received text: $text")

          val responseTxt: String =  text match {
            case "total" =>
              JsObject(Seq("total" -> JsNumber(assets.size))).toString()
            case "next" =>
              var asset: Option[Asset] = None
              var importAsset: Option[FileImportAsset] = None

              val assetResponseTxt = try {
                // get the first asset that we *can* import
                importAsset = None
                asset = None
                while (asset.isEmpty) {
                  if (!assetsIt.hasNext || criticalException.isDefined) throw new EndOfImportAssets
                  importAsset = Some(assetsIt.next())
                  asset = app.service.fileImport.importAsset(importAsset.get)
                }

                JsObject(Seq("asset" -> asset.get.toJson)).toString()
              }
              catch {
                case ex: DuplicateException =>
                  JsObject(Seq(
                    //FIXME: constants
                    "warning" -> JsString("Duplicate"),
                    "asset" -> ex.asset.toJson)).toString()
                case ex: EndOfImportAssets => "END"
                case ex: Throwable =>
                  importAsset.isDefined match {
                    case true => // import asset exists, we send the error and skip the asset
                      JsObject(Seq(
                        "error" -> JsString(ex.getMessage),
                        "importAsset" -> importAsset.get.toJson)).toString()
                    case false => // this is a critical error (not asset specific). We bail
                      criticalException = Some(ex)
                      JsObject(Seq("critical" -> JsString(ex.getMessage))).toString()
                  }
              }

              assetResponseTxt
          }
          log.info(responseTxt)
          this.send(responseTxt)
      }
    }
  }
}
