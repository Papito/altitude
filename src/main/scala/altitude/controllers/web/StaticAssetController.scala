package altitude.controllers.web

import java.io.File

import altitude.Environment
import org.slf4j.LoggerFactory

class StaticAssetController extends BaseWebController {
  private final val log = LoggerFactory.getLogger(getClass)

  get("/i/*") {
    serveFile("/i/")
  }

  get("/css/*") {
    contentType = "text/css"
    serveFile("/css/")
  }

  get("/js/*") {
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
}
