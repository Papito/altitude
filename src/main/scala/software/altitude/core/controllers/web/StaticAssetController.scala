package software.altitude.core.controllers.web

import java.io.File

import software.altitude.core.Environment
import org.slf4j.LoggerFactory

class StaticAssetController extends BaseWebController {
  private final val log = LoggerFactory.getLogger(getClass)

  before() {
    contentType = "application/octet-stream"
  }

  get("/i/*") {
    serveFile("/i/")
  }

  get("/html/*") {
    contentType = "text/html"
    serveFile("/html/")
  }

  get("/css/*") {
    contentType = "text/css"
    serveFile("/css/")
  }

  get("/js/*") {
    contentType = "application/javascript"
    serveFile("/js/")
  }

  private def serveFile(uriRoot: String): File = {
    val uriPath = uriRoot + params("splat")

    Environment.ENV match {
      case Environment.DEV => {
        val webAppPath = servletContext.getResource("/").getPath
        new File(webAppPath + "../../src/main/webapp" + uriPath)
      }
      case _ => {
        val resource = servletContext.getResource(uriPath)
        new File(resource.getPath)
      }
    }
  }

  override def setUser() = Unit
  override def logRequestStart() = Unit
  override def logRequestEnd() = Unit
}
