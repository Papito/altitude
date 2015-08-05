import javax.servlet.ServletContext

import altitude.Environment
import altitude.controllers.{AltitudeServlet, AssetServlet, ImportServlet}
import org.scalatra._
import org.slf4j.LoggerFactory

class ScalatraBootstrap extends LifeCycle {
  val log =  LoggerFactory.getLogger(getClass)

  override def init(context: ServletContext) {
    context.mount(new AltitudeServlet, "/*")
    context.mount(new ImportServlet, "/import/*")
    context.mount(new AssetServlet, "/assets/*")

    val environment = Environment.ENV match {
      case Environment.DEV => "development"
      case Environment.PROD => "production"
    }

    context.initParameters("org.scalatra.environment") = environment

  }
}
