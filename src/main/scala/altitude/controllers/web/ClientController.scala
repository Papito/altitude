package altitude.controllers.web

import java.io.{Writer, StringWriter, PrintWriter, File}

import altitude.Environment
import com.mitchellbosecke.pebble.PebbleEngine
import com.mitchellbosecke.pebble.template.PebbleTemplate
import org.slf4j.LoggerFactory

class ClientController  extends BaseWebController {
  private val log = LoggerFactory.getLogger(getClass)

  private lazy val templateRoot: File = Environment.ENV match {
    case Environment.DEV => {
      val webAppPath = servletContext.getResource("/").getPath
      new File(webAppPath + "/WEB-INF/client/templates")
    }
    case _ => {
      new File(Environment.root + "/client/templates")
    }
  }

  private val engine: PebbleEngine = new PebbleEngine.Builder().build()

  get("/*") {
    val templateFile = this.params("splat")
    log.debug(s"Client file request: $templateFile")
    val templateFilePath = s"${templateRoot.getAbsolutePath}/$templateFile.html"
    log.debug(s"Template file path: $templateFilePath")
    val compiledTemplate: PebbleTemplate = engine.getTemplate(templateFilePath)
    val writer: Writer = new StringWriter()
    compiledTemplate.evaluate(writer)
    writer.toString
  }


}
