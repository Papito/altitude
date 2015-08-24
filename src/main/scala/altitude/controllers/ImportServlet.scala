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

              val assetResponseTxt = try {
                if (!assetsIt.hasNext) throw new EndOfImportAssets
                val importAsset: FileImportAsset = assetsIt.next()

                //FIXME: go until end or valid asset
                asset = app.service.fileImport.importAsset(importAsset)

                JsObject(Seq("asset" -> asset.get.toJson)).toString()
              }
              catch {
                case ex: DuplicateException =>
                  JsObject(Seq(
                    //FIXME: constsants
                    "warning" -> JsString("Duplicate"),
                    "asset" -> ex.asset.toJson)).toString()
                case ex: EndOfImportAssets => "END"
                case ex: Throwable =>
                  JsObject(Seq("error" -> JsString(ex.getMessage))).toString()
              }

              assetResponseTxt
          }
          log.info(responseTxt)
          this.send(responseTxt)
      }
    }
  }
}
