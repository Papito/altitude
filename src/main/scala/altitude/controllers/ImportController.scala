package altitude.controllers

import java.io.File

import org.json4s._
import org.scalatra._
import org.scalatra.atmosphere._
import org.scalatra.json.{JValueResult, JacksonJsonSupport}
import org.scalatra.servlet.{FileItem, SizeConstraintExceededException, MultipartConfig, FileUploadSupport}
import org.slf4j.LoggerFactory
import play.api.libs.json.{Json, JsNumber, JsObject, JsString}

import scala.concurrent.ExecutionContext.Implicits.global

import altitude.{Const => C}
import altitude.exceptions.{DuplicateException, StopImport}
import altitude.models.{Asset, FileImportAsset}


class ImportController extends BaseController  with JValueResult
with JacksonJsonSupport with SessionSupport with AtmosphereSupport with FileUploadSupport  {
  val ONE_HUNDRED_MEGABYTES = 1024 * 1024 * 100
  configureMultipartHandling(MultipartConfig(maxFileSize = Some(ONE_HUNDRED_MEGABYTES)))

  val log = LoggerFactory.getLogger(getClass)
  implicit protected val jsonFormats: Formats = DefaultFormats

  get("/") {
    contentType="text/html"
    ssp("/import")
  }

  post("/") {
    val files: Seq[FileItem] = fileMultiParams("files[]")
  }

  get("/source/local/navigate") {
    val path: String = this.params.getOrElse(C.Api.PATH, "/")
    log.debug(s"Getting directory name list for $path")
    val files: Seq[File] = new File(path).listFiles().toSeq
    val directoryList: Seq[String] = files.filter(_.isDirectory == true).map(_.getName)
    Json.obj(
      C.Api.DIRECTORY_NAMES -> directoryList,
      C.Api.CURRENT_PATH -> path)
  }

  atmosphere("/ws") {
    new AtmosphereClient {
      var assets: Option[List[FileImportAsset]] = None
      var assetsIt: Option[Iterator[FileImportAsset]] = None
      var criticalException: Option[Throwable] = None
      var path: Option[String] = None

      def receive: AtmoReceive = {
        case txt: TextMessage if txt.content.startsWith("total") =>
          log.info(s"WS -> $txt")

          // get the path we will be importing from
          //FIXME: not defensive
          val separatorIdx = txt.content.indexOf(' ')
          path = Some(txt.content.substring(separatorIdx + 1))


          assets = Some(app.service.fileImport.getFilesToImport(path=path.get))
          assetsIt = Some(assets.get.toIterator)
          val responseTxt = JsObject(Seq("total" -> JsNumber(assets.get.size))).toString()
          log.info(s"WS <- $responseTxt")
          this.send(responseTxt)

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
                if (!assetsIt.get.hasNext || criticalException.isDefined) throw new StopImport
                importAsset = Some(assetsIt.get.next())
                asset = app.service.fileImport.importAsset(importAsset.get)
              }

              JsObject(Seq(C.Api.Asset.ASSET -> asset.get.toJson)).toString()
            }
            catch {
              case ex: StopImport => "END"
              case ex: DuplicateException => {
                JsObject(Seq(
                  C.Api.WARNING -> JsString(C.MSG("warn.duplicate")),
                  C.Api.Asset.ASSET -> ex.asset.toJson)).toString()
              }
              case ex: Throwable => {
                importAsset.isDefined match {
                  case true => // import asset exists, we send the error and skip the asset
                    JsObject(Seq(
                      C.Api.ERROR -> JsString(ex.getMessage),
                      C.Api.ImportAsset.IMPORT_ASSET -> importAsset.get.toJson)).toString()
                  case false => // this is a critical error (not asset specific). We bail
                    criticalException = Some(ex)
                    JsObject(Seq(C.Api.CRITICAL -> JsString(ex.getMessage))).toString()
                }
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
