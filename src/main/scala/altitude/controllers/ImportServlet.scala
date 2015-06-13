package altitude.controllers

import altitude.models.{Asset, FileImportAsset}
import org.slf4j.LoggerFactory

import org.json4s._
import org.scalatra._
import org.scalatra.atmosphere._
import org.scalatra.json.{JValueResult, JacksonJsonSupport}
import play.api.libs.json.{JsNumber, JsString, JsValue, JsObject}
import scala.concurrent.ExecutionContext.Implicits.global

class ImportServlet extends BaseController  with JValueResult
with JacksonJsonSupport with SessionSupport with AtmosphereSupport  {
  val log = LoggerFactory.getLogger(getClass)
  implicit protected val jsonFormats: Formats = DefaultFormats

  get("/") {
    app.txManager.withTransaction {
      app.txManager.withTransaction {
        app.txManager.asReadOnly {
          log.info("I AM IN A TRANSACTION, BITCHES!")
        }
      }
    }

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
              val asset: Option[Asset] = app.service.fileImport.importAsset(importAsset)

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

  error {
    case t: Throwable => t.printStackTrace()
  }

  notFound {
    // remove content type in case it was set through an action
    contentType = null
    // Try to render a ScalateTemplate if no route matched
    findTemplate(requestPath) map { path =>
      contentType = "text/html"
      layoutTemplate(path)
    } orElse serveStaticResource() getOrElse resourceNotFound()
  }

}
