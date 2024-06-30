package software.altitude.core.controllers.web

import org.json4s.{DefaultFormats, Formats}
import org.scalatra.{RequestEntityTooLarge, SessionSupport}
import org.scalatra.atmosphere.{AtmoReceive, AtmosphereClient, AtmosphereSupport, Connected, Disconnected, Error, JsonMessage, TextMessage}
import org.scalatra.json.{JValueResult, JacksonJsonSupport}
import org.scalatra.servlet.{FileUploadSupport, MultipartConfig, SizeConstraintExceededException}
import software.altitude.core.{DuplicateException, RequestContext}
import software.altitude.core.controllers.BaseWebController
import software.altitude.core.models.{ImportAsset, Metadata}
import scala.concurrent.ExecutionContext.Implicits.global

class ImportController extends BaseWebController with FileUploadSupport with AtmosphereSupport   with JValueResult
  with JacksonJsonSupport with SessionSupport {
  private val fileSizeLimitGB = 10

  implicit protected val jsonFormats: Formats = DefaultFormats

  private val userToWsClientLookup = collection.mutable.Map[String, List[AtmosphereClient]]()

  configureMultipartHandling(MultipartConfig(maxFileSize = Some(fileSizeLimitGB*1024*1024)))

  before() {
    requireLogin()
  }

  get("/") {
    contentType = "text/html"
    layoutTemplate(
      "/WEB-INF/templates/views/import.ssp",
      // TODO: move into CONSTANTS
      "userId" -> RequestContext.getAccount.persistedId
    )
  }

  atmosphere("/status") {
    val userId = params("userId")

    val client = new AtmosphereClient {
      def receive: AtmoReceive = {
        case Connected =>
          logger.info("Client connected ...")
          logger.info(s"Associating user $userId with client $uuid")

          if (!userToWsClientLookup.contains(userId)) {
            userToWsClientLookup += userId -> List(this)
          } else {
            userToWsClientLookup(userId) = this :: userToWsClientLookup(userId)
          }

        case Disconnected(disconnector, Some(error)) =>
          logger.info(s"Disassociating user $userId with client $uuid")
          userToWsClientLookup.get(userId).foreach { clients =>
            userToWsClientLookup(userId) = clients.filterNot(_ == this)
          }
        case Error(Some(error)) =>
            logger.error(s"WS Error: $error")

        case TextMessage(text) =>
        case JsonMessage(json) =>
      }
    }

    client
  }

  post("/upload") {
    contentType = "text/html"

    fileMultiParams.get("files") match {
      case Some(files) => files.zipWithIndex.foreach { case (file, index) =>
          logger.info(s"Received file: $file")

          val importAsset = new ImportAsset(
            fileName = file.getName,
            data = file.get(),
            metadata = Metadata())

        // To wipe the status ticker on the client clean
        val isLastFile = index == files.length - 1

        app.executorService.submit(new Runnable {
          override def run(): Unit = {
            try {
              app.service.assetImport.importAsset(importAsset)
              sendWsStatusToUserClients(message=s"<div id=\"status\">${importAsset.fileName}</div>")
            } catch {
              case _: DuplicateException =>
                logger.warn(s"Duplicate asset: ${importAsset.fileName}")

                sendWsStatusToUserClients(s"<div id=\"status\">DUPLICATE ${importAsset.fileName}</div>")
              case e: Exception => logger.error("Error importing asset:", e)
            }

            if (isLastFile) {
              sendWsStatusToUserClients(message = "<div id=\"status\"></div>")
            }
          }
        })
      }
      case None =>
        logger.warn("No files received for upload")
        halt(200, layoutTemplate("/WEB-INF/templates/views/htmx/upload_form.ssp"))
    }

    layoutTemplate("/WEB-INF/templates/views/htmx/upload_form.ssp")
  }

  private def sendWsStatusToUserClients(message: String): Unit = {
    userToWsClientLookup.get(RequestContext.getAccount.persistedId).foreach { clients =>
      clients.foreach { client =>client.send(message)
      }
    }
  }

  error {
    case _: SizeConstraintExceededException =>
      RequestEntityTooLarge(s"File size exceeds ${fileSizeLimitGB}GB")
  }
}
