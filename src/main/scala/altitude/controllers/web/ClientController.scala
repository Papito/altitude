package altitude.controllers.web

import java.io.{StringWriter, PrintWriter, File}

import altitude.Environment
import com.github.mustachejava.{Mustache, DefaultMustacheFactory}
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

  private lazy val mustacheFactory = new DefaultMustacheFactory(templateRoot)

  get("/*") {
    val templateFile = this.params("splat")
    log.debug(s"Client file request: $templateFile")
    val mustache: Mustache = mustacheFactory.compile(templateFile + ".mustache")
    val sw: StringWriter = new StringWriter()
    val pw: PrintWriter = new PrintWriter(sw)
    mustache.execute(new PrintWriter(pw), List()).flush()

    sw.toString
  }


}
