package software.altitude.core.controllers.web

import org.scalatra.RequestEntityTooLarge
import org.scalatra.servlet.{FileUploadSupport, MultipartConfig, SizeConstraintExceededException}
import software.altitude.core.controllers.BaseWebController
import software.altitude.core.models.{ImportAsset, Metadata}

import java.util.concurrent.{ExecutorService, Executors}

class ImportController extends BaseWebController with FileUploadSupport {
  private val fileSizeLimitGB = 10

  configureMultipartHandling(MultipartConfig(maxFileSize = Some(fileSizeLimitGB*1024*1024)))
  private val executorService: ExecutorService = Executors.newFixedThreadPool(6)

  before() {
    requireLogin()
  }

  get("/") {
    contentType = "text/html"
    layoutTemplate("/WEB-INF/templates/views/import.ssp")
  }

  post("/upload") {
    contentType = "text/html"

    fileMultiParams.get("files") match {
      case Some(files) => files.foreach { file =>
          logger.info(s"Received file: $file")

          val importAsset = new ImportAsset(
            fileName = file.getName,
            data = file.get(),
            metadata = Metadata())

        executorService.submit(new Runnable {
          override def run(): Unit = {
            try {
              app.service.assetImport.importAsset(importAsset)
            } catch {
              case e: Exception => logger.error("Error importing asset", e)
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

  error {
    case _: SizeConstraintExceededException =>
      RequestEntityTooLarge(s"File size exceeds ${fileSizeLimitGB}GB")
  }
}
