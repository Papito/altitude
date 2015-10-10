package altitude.controllers

import java.io.File

import altitude.controllers.web.BaseWebController
import altitude.exceptions.{MetadataExtractorException, DuplicateException, AllDone}
import altitude.models.{FileImportAsset, Asset}
import altitude.{Const => C}
import org.json4s.JsonAST.JObject
import org.json4s._
import JsonDSL._
import org.scalatra._
import org.scalatra.atmosphere._
import org.scalatra.json.{JValueResult, JacksonJsonSupport}
import org.scalatra.servlet.{FileUploadSupport, MultipartConfig, SizeConstraintExceededException}
import org.slf4j.LoggerFactory
import play.api.libs.json._

import scala.concurrent.ExecutionContext.Implicits.global

class ImportController extends BaseWebController  with JValueResult
with JacksonJsonSupport with SessionSupport with AtmosphereSupport with FileUploadSupport  {
  private final val log = LoggerFactory.getLogger(getClass)

  val ONE_HUNDRED_MEGABYTES = 1024 * 1024 * 100
  configureMultipartHandling(MultipartConfig(maxFileSize = Some(ONE_HUNDRED_MEGABYTES)))

  implicit protected val jsonFormats: Formats = DefaultFormats

  post("/") {
    //val files: Seq[FileItem] = fileMultiParams("files[]")
  }

  get("/source/local/navigate") {
    val path: String = this.params.getOrElse(C.Api.PATH, "/")
    log.debug(s"Getting directory name list for $path")
    val files: Seq[File] = new File(path).listFiles().toSeq
    val directoryList: Seq[String] = files.filter(_.isDirectory == true).map(_.getName)
    contentType = "application/json"
    Json.obj(
      C.Api.DIRECTORY_NAMES -> directoryList,
      C.Api.CURRENT_PATH -> path).toString()
  }

  atmosphere("/ws") {
    new AtmosphereClient {
      private def uuidJson: JObject = "uid" -> uuid
      private var stopImport = false

      var assets: Option[List[FileImportAsset]] = None
      var assetsIt: Option[Iterator[FileImportAsset]] = None
      var path: Option[String] = None

      private def writeToYou(jsonMessage: JValue): Unit = {
        log.info(s"YOU -> $jsonMessage")
        this.send(jsonMessage)
      }

      private def writeToAll(jsonMessage: JValue): Unit = {
        log.info(s"ALL -> $jsonMessage")
        this.broadcast(jsonMessage, Everyone)
      }

      private def writeToRest(jsonMessage: JValue): Unit = {
        log.info(s"REST -> $jsonMessage")
        this.broadcast(jsonMessage)
      }

      def receive: AtmoReceive = {
        case message @ JsonMessage(JObject(JField("action", JString("getUID")) :: fields)) => {
          val json: JValue = message.content
          log.info(s"WS <- $json")
          this.writeToYou(uuidJson)
        }

        case message @ JsonMessage(JObject(JField("action", JString("getFileCount")) :: fields)) => {
          val json: JValue = message.content
          log.info(s"WS <- $json")

          // get the path we will be importing from
          // FIXME: not defensive
          val path: String = (json \ "path").extract[String]

          assets = Some(app.service.fileImport.getFilesToImport(path = path))
          assetsIt = Some(assets.get.toIterator)

          this.writeToYou("total" -> assets.get.size)
        }

        case message @ JsonMessage(JObject(JField("action", JString("startImport")) :: fields)) => {
          val json: JValue = message.content
          log.info(s"WS <- $json")
        }

        case message @ JsonMessage(JObject(JField("action", JString("stopImport")) :: fields)) => {
          val json: JValue = message.content
          log.info(s"WS <- $json")
          this.writeToYou("end" -> true)
        }

        case TextMessage("next") => {
          log.info("WS -> next")
          val responseTxt: String = {
            var asset: Option[Asset] = None
            var importAsset: Option[FileImportAsset] = None

            try {
              // get the first asset that we *can* import
              importAsset = None
              asset = None

              while (asset.isEmpty) {
                if (!assetsIt.get.hasNext)
                  throw new AllDone

                importAsset = Some(assetsIt.get.next())
                asset = app.service.fileImport.importAsset(importAsset.get)
              }

              JsObject(Seq(
                C.Api.Asset.ASSET -> asset.get.toJson,
                C.Api.Import.IMPORTED -> JsBoolean(true)
              )).toString()
            }
            catch {
              case ex: AllDone => "END"
              case ex: DuplicateException => {
                JsObject(Seq(
                  C.Api.WARNING -> JsString(C.MSG("warn.duplicate")),
                  C.Api.Asset.ASSET -> ex.asset.toJson,
                  C.Api.Import.IMPORTED -> JsBoolean(false)
                )).toString()
              }
              case ex: MetadataExtractorException => {
                JsObject(Seq(
                  C.Api.WARNING -> JsString(
                    s"Metadata parser(s) failed. Asset still imported"),
                  C.Api.Asset.ASSET -> ex.asset.toJson,
                  C.Api.Import.IMPORTED -> JsBoolean(true))).toString()
              }
              case ex: Exception => {
                ex.printStackTrace()

                val importAssetJson = importAsset match {
                  case None => JsNull
                  case _ => importAsset.get.toJson
                }

                JsObject(Seq(
                  C.Api.ERROR -> JsString(ex.toString),
                  C.Api.Import.IMPORTED -> JsBoolean(false),
                  C.Api.ImportAsset.IMPORT_ASSET -> importAssetJson
                )).toString()
              }
            }
          }
          log.info(s"WS <- $responseTxt")
          this.send(responseTxt)
        } // end "next"

        case TextMessage(data: String) =>
          log.info(s"WS -> $data")

        case Connected =>
          log.info("Client connected")

        case Disconnected(disconnector, Some(error)) =>
          log.info("Client disconnected")

        case Error(Some(error)) =>
          // FIXME: log
          error.printStackTrace()

      }
    }
  }

  error {
    case e: SizeConstraintExceededException => RequestEntityTooLarge(s"sFile is larger than $ONE_HUNDRED_MEGABYTES")
  }
}
