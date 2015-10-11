package altitude.controllers

import java.io.File

import altitude.controllers.web.BaseWebController
import altitude.exceptions.{MetadataExtractorException, DuplicateException, AllDone}
import altitude.models.{FileImportAsset, Asset}
import altitude.{Const => C}
import org.json4s.JsonAST.JObject
import org.json4s._
import org.scalatra._
import org.scalatra.atmosphere._
import org.scalatra.json.{JValueResult, JacksonJsonSupport}
import org.scalatra.servlet.{FileUploadSupport, MultipartConfig, SizeConstraintExceededException}
import org.slf4j.LoggerFactory
import play.api.libs.json._
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Success, Failure}

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
      private def uuidJson: JsValue = JsObject(Seq("uid" -> JsString(uuid)))
      private var stopImport = false

      var assets: Option[List[FileImportAsset]] = None
      var assetsIt: Option[Iterator[FileImportAsset]] = None
      var path: Option[String] = None

      private def writeToYou(jsonMessage: JsValue): Unit = {
        log.info(s"YOU -> $jsonMessage")
        this.send(jsonMessage.toString())
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
          // FIXME: not defensive enough, check if defined
          val path: String = (json \ "path").extract[String]

          assets = Some(app.service.fileImport.getFilesToImport(path = path))
          assetsIt = Some(assets.get.toIterator)

          this.writeToYou(JsObject(Seq("total" -> JsNumber(assets.get.size))))
        }

        case message @ JsonMessage(JObject(JField("action", JString("stopImport")) :: fields)) => {
          val json: JValue = message.content
          log.info(s"WS <- $json")
          stopImport = true
          this.writeToYou(JsObject(Seq("end" -> JsBoolean(true))))
        }

        case message @ JsonMessage(JObject(JField("action", JString("startImport")) :: fields)) => {
          val json: JValue = message.content
          log.info(s"WS <- $json")

          Future {
            try {
              importAssets()
            }
            catch {
              case ex: AllDone => {
                stopImport = false
                this.writeToYou(JsObject(Seq("end" -> JsBoolean(true))))
              }
            }
          }
        }

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

      private def importAssets() = {
        case class NotImportable() extends Exception

        while (assetsIt.get.hasNext && !stopImport) {
          val importAsset: Option[FileImportAsset] = Some(assetsIt.get.next())

          try {
            val asset: Option[Asset] = app.service.fileImport.importAsset(importAsset.get)
            if (asset.isEmpty) throw NotImportable()

            val resp = JsObject(Seq(
              C.Api.Asset.ASSET -> asset.get.toJson,
              C.Api.Import.IMPORTED -> JsBoolean(true)
            ))
            this.writeToYou(resp)
          }
          catch {
            case ex: NotImportable => {/* next */}
            case ex: DuplicateException => {
              val resp =JsObject(Seq(
                C.Api.WARNING -> JsString(C.MSG("warn.duplicate")),
                C.Api.Asset.ASSET -> ex.asset.toJson,
                C.Api.Import.IMPORTED -> JsBoolean(false)
              ))
              this.writeToYou(resp)
            }
            case ex: MetadataExtractorException => {
              val resp =JsObject(Seq(
                C.Api.WARNING -> JsString(
                  s"Metadata parser(s) failed. Asset still imported"),
                C.Api.Asset.ASSET -> ex.asset.toJson,
                C.Api.Import.IMPORTED -> JsBoolean(true)))
              this.writeToYou(resp)
            }
            case ex: Exception => {
              ex.printStackTrace()

              val importAssetJson = importAsset match {
                case None => JsNull
                case _ => importAsset.get.toJson
              }

              val resp = JsObject(Seq(
                C.Api.ERROR -> JsString(ex.toString),
                C.Api.Import.IMPORTED -> JsBoolean(false),
                C.Api.ImportAsset.IMPORT_ASSET -> importAssetJson
              ))
              this.writeToYou(resp)
            }
          }
        }

        throw new AllDone
      }
    }
  }

  error {
    case e: SizeConstraintExceededException => RequestEntityTooLarge(s"sFile is larger than $ONE_HUNDRED_MEGABYTES")
  }
}
