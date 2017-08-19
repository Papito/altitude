package software.altitude.core.controllers

import java.io.{File, PrintWriter, StringWriter}
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

import software.altitude.core.models.{Asset, ImportAsset}
import software.altitude.core.{Const => C, AllDone, MetadataExtractorException, DuplicateException, Context}
import org.json4s.JsonAST.{JField, JObject, JString}
import org.json4s.{DefaultFormats, Formats, JValue, _}
import org.scalatra.atmosphere._
import org.scalatra.json.{JValueResult, JacksonJsonSupport}
import org.scalatra.servlet.{FileUploadSupport, MultipartConfig, SizeConstraintExceededException}
import org.scalatra.{SessionSupport, _}
import org.slf4j.LoggerFactory
import play.api.libs.json._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class ImportController extends BaseController with JValueResult
with JacksonJsonSupport with SessionSupport with AtmosphereSupport  with FileUploadSupport  {
  private final val log = LoggerFactory.getLogger(getClass)

  val ONE_HUNDRED_MEGABYTES = 1024 * 1024 * 100
  configureMultipartHandling(MultipartConfig(maxFileSize = Some(ONE_HUNDRED_MEGABYTES)))

  implicit protected val jsonFormats: Formats = DefaultFormats

  post("/") {
    //val files: Seq[FileItem] = fileMultiParams("files[]")
  }

  private val userHomeDir = System.getProperty("user.home")

  get("/fs/listing") {
    val file: File = new File(this.params.getOrElse(C.Api.PATH, userHomeDir))
    log.debug(s"Getting directory name list for $file")
    val files: Seq[File] = file.listFiles().toSeq
    val directoryList: Seq[String] = files.filter(_.isDirectory == true).map(_.getName)
    contentType = "application/json"
    Json.obj(
      C.Api.DIRECTORY_NAMES -> directoryList,
      C.Api.CURRENT_PATH -> file.getAbsolutePath,
      C.Api.OS_DIR_SEPARATOR -> File.separator).toString()
  }

  get("/fs/listing/parent") {
    val path = this.params.getOrElse(C.Api.PATH, userHomeDir)
    log.debug(s"Path: $path")
    val file: File = new File(this.params.getOrElse(C.Api.PATH, userHomeDir))
    val parentPath: String = file.getParent
    log.debug(s"Parent path: $parentPath")

    val parentFile = if (parentPath != null) new File(parentPath) else file

    log.debug(s"Getting directory name list for $file")
    val files: Seq[File] = parentFile.listFiles().toSeq
    val directoryList: Seq[String] = files.filter(_.isDirectory == true).map(_.getName)
    contentType = "application/json"
    Json.obj(
      C.Api.DIRECTORY_NAMES -> directoryList,
      C.Api.CURRENT_PATH -> parentFile.getAbsolutePath).toString()
  }

  atmosphere("/fs/ws") {
    new AtmosphereClient {
      private def uuidJson: JsValue = Json.obj("uid" -> JsString(uuid))
      private var stopImport = false
      private case class NotImportable() extends Exception
      private val workerNum: AtomicInteger = new AtomicInteger(0)

      @volatile var assetsIt: Option[Iterator[ImportAsset]] = None
      var path: Option[String] = None

      implicit val context: Context = new Context(repo = app.REPO, user = app.USER)

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
          val path: String = (json \ "path").extract[String]

          assetsIt = Some(app.service.source.fileSystem.assetIterator(path))

          val count = app.service.source.fileSystem.count(path)
          this.writeToYou(Json.obj("total" -> JsNumber(count)))
        }

        case message @ JsonMessage(JObject(JField("action", JString("stopImport")) :: fields)) => {
          val json: JValue = message.content
          log.info(s"WS <- $json")
          stopImport = true
        }

        case message @ JsonMessage(JObject(JField("action", JString("startImport")) :: fields)) => {
          val json: JValue = message.content
          log.info(s"WS <- $json")

          val numWorkers = sys.runtime.availableProcessors
          val pool = Executors.newFixedThreadPool(numWorkers)
          implicit val ec = ExecutionContext.fromExecutorService(pool)

          if (stopImport) throw new IllegalStateException("Still shutting down")

          for (threadIdx <- 1 to numWorkers) {
            log.debug(s"About to start worker: $threadIdx")
            Future {
              runImportWorker(threadIdx)
            } onFailure {
              case allDone: AllDone => cleanupWorker()
              case ex: Exception =>
                cleanupWorker()
                log.error("Unknown worker exception: $ex")
            }
          }
        }

        case TextMessage(data: String) =>
          log.info(s"WS -> $data")

        case Connected =>
          log.info("Client connected")

        case Disconnected(disconnector, Some(error)) =>
          log.info("Client disconnected" + disconnector)

        case Error(Some(error)) =>
          val sw: StringWriter = new StringWriter()
          val pw: PrintWriter = new PrintWriter(sw)
          error.printStackTrace(pw)
          error.printStackTrace()
          log.error(s"Error: ${error.getMessage}. ${sw.toString}")

      }

      private def cleanupWorker() = {
        // decrement worker and see what we have left
        val workersNow = workerNum.decrementAndGet()
        log.info(s"Workers left: $workersNow")
        if (workersNow == 0) {
          log.debug(s"ALL DONE")
          stopImport = false
          this.writeToYou(Json.obj("end" -> JsBoolean(true)))
        }
      }

      private def runImportWorker(workerIdx: Int) = {
        log.info(s"Started import worker $workerIdx")
        workerNum.incrementAndGet()

        while (assetsIt.get.hasNext && !stopImport) {
          val importAsset: ImportAsset = Some(assetsIt.get.next()).get
          log.info(s"Processing import asset $importAsset")

          try {
            val asset: Option[Asset] = app.service.assetImport.importAsset(importAsset)
            if (asset.isEmpty) throw NotImportable()

            val resp = Json.obj(
              C.Api.Asset.ASSET -> asset.get.toJson,
              C.Api.Import.IMPORTED -> JsBoolean(true))
            this.writeToYou(resp)
          }
          catch {
            case ex: NotImportable =>
            case ex: DuplicateException =>
              log.warn(s"Duplicate for $importAsset")
              val duplicateOf: Asset = ex.duplicateOf
              val resp = Json.obj(
                C.Api.WARNING -> JsString(C.Msg.Warn.DUPLICATE + " of " + duplicateOf.path),
                C.Api.Asset.ASSET -> ex.objJson,
                C.Api.DUPLICATE_OF -> ex.duplicateOf,
                C.Api.Import.IMPORTED -> JsBoolean(false))
              this.writeToYou(resp)

            case ex: MetadataExtractorException =>
              log.warn(s"Metadata extraction error for $importAsset")
              val resp =Json.obj(
                C.Api.WARNING -> JsString(
                  s"Metadata parser(s) failed. Asset still imported"),
                C.Api.Asset.ASSET -> ex.asset.toJson,
                C.Api.Import.IMPORTED -> JsBoolean(true))
              this.writeToYou(resp)

            case ex: Exception =>
              log.error(s"Import error for $importAsset")
              val sw: StringWriter = new StringWriter()
              val pw: PrintWriter = new PrintWriter(sw)
              ex.printStackTrace(pw)

              log.error(s"${ex.getClass.getName} exception: ${sw.toString}")

              val resp = Json.obj(
                C.Api.ERROR -> JsString(ex.toString),
                C.Api.Import.IMPORTED -> JsBoolean(false),
                C.Api.ImportAsset.IMPORT_ASSET -> importAsset.toJson)
              this.writeToYou(resp)
          }
        }

        log.info(s"Import worker $workerIdx done iterating")
        throw AllDone()
      }
    }
  }

  error {
    case e: SizeConstraintExceededException => RequestEntityTooLarge(s"sFile is larger than $ONE_HUNDRED_MEGABYTES")
  }
}
