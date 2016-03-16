package altitude.controllers.web

import java.io.{File, StringWriter, Writer}

import altitude.{Const, Environment}
import com.mitchellbosecke.pebble.PebbleEngine
import com.mitchellbosecke.pebble.template.PebbleTemplate
import org.slf4j.LoggerFactory
import scala.collection.JavaConversions._

class ClientController  extends BaseWebController {
  private val log = LoggerFactory.getLogger(getClass)

  private lazy val templateRoot: File = Environment.ENV match {
    case Environment.DEV => {
      val webAppPath = servletContext.getResource("/").getPath
      new File(webAppPath + "../../src/main/webapp/WEB-INF/client/templates")
    }
    case _ => {
      new File(Environment.root + "/client/templates")
    }
  }

  private val builder = new PebbleEngine.Builder()
  builder.cacheActive(false) // TODO: this should be a setting in prod
  private val engine: PebbleEngine = builder.build()

  before() {
    contentType = "text/html; charset=UTF-8"
  }

  private lazy val CONTEXT = Map[String, AnyRef](
      "Const" -> mapAsJavaMap(Const.data),
      "Config" -> mapAsJavaMap(app.config.data)
    )

  get("/*") {
    val templateFile = this.params("splat")
    log.debug(s"Client file request: $templateFile")
    val templateFilePath = s"${templateRoot.getAbsolutePath}/$templateFile.html"
    log.debug(s"Template file path: $templateFilePath")
    val compiledTemplate: PebbleTemplate = engine.getTemplate(templateFilePath)
    val writer: Writer = new StringWriter()

    compiledTemplate.evaluate(writer, CONTEXT)
    writer.toString
  }


}
