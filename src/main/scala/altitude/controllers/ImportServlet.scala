package altitude.controllers

import altitude.exceptions.DuplicateException
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
    val assets = app.service.fileImport.getFilesToImport(path="/mnt/hgfs/import/")
    val assetsIt = assets.toIterator

    new AtmosphereClient {
      def receive = {
        case Connected =>
          log.info("Client connected")
        case Disconnected(disconnector, Some(error)) =>
          log.info("Client disconnected")
        case Error(Some(error)) =>
        case TextMessage(text) =>
          log.info(s"Received text: $text")
          text match {
            case "total" =>
              val jsonOut = JsObject(Seq("total" -> JsNumber(assets.size)))
              val dataOut = jsonOut.toString()
              log.info(dataOut)
              this.send(dataOut)
            case "next" =>

              val importAsset: FileImportAsset = assetsIt.next()

              var asset: Option[Asset] = None

              try {
                asset = app.service.fileImport.importAsset(importAsset)
              }
              catch {
                case ex: DuplicateException =>
                  val dataOut = JsObject(Seq("warning" -> JsString("Duplicate"))).toString()
                  log.info(dataOut)
                  this.send(dataOut)
                case ex: Throwable =>
                  ex.printStackTrace()
                  val dataOut = JsObject(Seq("error" -> JsString(ex.getMessage))).toString()
                  log.info(dataOut)
                  this.send(dataOut)
              }

              val jsonOut = asset.isDefined match {
                case true => JsObject(Seq("asset" -> asset.get.toJson))
                case false => JsObject(Seq("warning" -> JsString("Skipping")))
              }
              val dataOut = jsonOut.toString()
              log.info(dataOut)
              this.send(dataOut)
          }
      }
    }
  }
}
