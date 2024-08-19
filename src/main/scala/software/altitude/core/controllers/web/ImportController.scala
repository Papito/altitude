package software.altitude.core.controllers.web

import org.apache.commons.fileupload.servlet.ServletFileUpload
import org.apache.commons.io.IOUtils
import org.json4s.DefaultFormats
import org.json4s.Formats
import org.scalatra.RequestEntityTooLarge
import org.scalatra.Route
import org.scalatra.SessionSupport
import org.scalatra.atmosphere.AtmoReceive
import org.scalatra.atmosphere.AtmosphereClient
import org.scalatra.atmosphere.AtmosphereSupport
import org.scalatra.atmosphere.Connected
import org.scalatra.atmosphere.Disconnected
import org.scalatra.atmosphere.Error
import org.scalatra.atmosphere.JsonMessage
import org.scalatra.atmosphere.TextMessage
import org.scalatra.json.JValueResult
import org.scalatra.json.JacksonJsonSupport
import org.scalatra.servlet.SizeConstraintExceededException
import software.altitude.core.DuplicateException
import software.altitude.core.RequestContext
import software.altitude.core.controllers.BaseWebController
import software.altitude.core.models.ImportAsset
import software.altitude.core.models.Metadata

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import scala.concurrent.ExecutionContext.Implicits.global

class ImportController
  extends BaseWebController
    with AtmosphereSupport
    with JValueResult
    with JacksonJsonSupport
    with SessionSupport {
  private val fileSizeLimitGB = 10

  private val importAssetCountPerRepo = new ConcurrentHashMap[String, (AtomicInteger, AtomicInteger)]()

  implicit protected val jsonFormats: Formats = DefaultFormats

  private val userToWsClientLookup = collection.mutable.Map[String, List[AtmosphereClient]]()

  // HTMX WS client expects a div with id "status" to update the status ticker
  private val successStatusTickerTemplate = "<div id=\"statusText\">%s</div>"
  private val warningStatusTickerTemplate = "<div id=\"statusText\" class=\"warning\">%s</div>"
  private val errorStatusTickerTemplate = "<div id=\"statusText\" class=\"error\">%s</div>"

  // I don't feel strongly about this
  // configureMultipartHandling(MultipartConfig(maxFileSize = Some(fileSizeLimitGB*1024*1024)))

  before() {
    requireLogin()
  }

  val importView: Route = get("/r/:repoId") {
    contentType = "text/html"
    layoutTemplate("/WEB-INF/templates/views/import.ssp")
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
          val clients = userToWsClientLookup(userId)
          userToWsClientLookup(userId) = clients.filterNot(_ == this)

        case Error(Some(error)) =>
            logger.error(s"WS Error: $error")

        case TextMessage(text) =>
        case JsonMessage(json) =>
      }
    }

    client
  }

  val uploadFilesForm: Route = post("/r/:repoId/upload") {
    contentType = "text/html"

    val repoId = RequestContext.getRepository.persistedId

    val sfu = new ServletFileUpload()
    sfu.setFileSizeMax(fileSizeLimitGB * 1024 * 1024)

    if (!ServletFileUpload.isMultipartContent(request)) {
      logger.error("Not a multipart upload")
      halt(200, layoutTemplate("/WEB-INF/templates/views/htmx/upload_form.ssp"))
    }

    val processedAndTotal = importAssetCountPerRepo.computeIfAbsent(repoId, _ =>
      (new AtomicInteger(0), new AtomicInteger()) )

    val iter = sfu.getItemIterator(request)

    while (iter.hasNext) {
      val fileItemStream = iter.next()
      val fStream = fileItemStream.openStream()
      val bytes = IOUtils.toByteArray(fStream)
      logger.info(s"Received file: ${fileItemStream.getName}]")

      processedAndTotal._2.addAndGet(1)

      val importAsset = new ImportAsset(
        fileName = fileItemStream.getName,
        data = bytes,
        metadata = Metadata())

      app.executorService.submit(new Runnable {
        override def run(): Unit = {
          try {
            app.service.assetImport.importAsset(importAsset)

            // will be updated in the finally block
            val processedSoFar = processedAndTotal._1.get() + 1
            val total = processedAndTotal._2.get()

            val importedStatusText = s"Imported $processedSoFar of $total: <span>${importAsset.fileName}</span>"
            sendWsStatusToUserClients(successStatusTickerTemplate.format(importedStatusText))

          } catch {

            case _: DuplicateException =>
              logger.warn(s"Duplicate asset: ${importAsset.fileName}")
              val duplicateStatusText = s"Ignoring duplicate: <span>${importAsset.fileName}</span>"
              sendWsStatusToUserClients(warningStatusTickerTemplate.format(duplicateStatusText))

            case e: Exception =>
              logger.error("Error importing asset:", e)

              val errorStatusText = s"Error: <span>${importAsset.fileName}</span>"
              sendWsStatusToUserClients(errorStatusTickerTemplate.format(errorStatusText))

          } finally {
            processedAndTotal._1.incrementAndGet()

            if (processedAndTotal._1.get() == processedAndTotal._2.get()) {
              processedAndTotal._1.set(0)
              processedAndTotal._2.set(0)

              // save the model after all files have been processed
              app.service.faceRecognition.saveModel()

              sendWsStatusToUserClients(
                successStatusTickerTemplate.format("All files processed"))
            }
          }
        }
      })

      fStream.close()
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
