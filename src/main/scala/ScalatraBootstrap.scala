import javax.servlet.ServletContext

import altitude._
import altitude.controllers.{ImportServlet, AltitudeServlet}
import org.scalatra._
import org.slf4j.LoggerFactory

class ScalatraBootstrap extends LifeCycle {
  val log =  LoggerFactory.getLogger(getClass)
  var app: Option[Altitude] = None

  override def init(context: ServletContext) {
    log.info("Initializing ... ")
    app = new Some(new Altitude(isProd = true, isTest = false))
    context.mount(new AltitudeServlet, "/*")
    context.mount(new ImportServlet, "/import/*")
  }
  override def destroy(context: ServletContext): Unit = {
    log.info("Cleaning up ... ")
    super.destroy(context)
  }
}
