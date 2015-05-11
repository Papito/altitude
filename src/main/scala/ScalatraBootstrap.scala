import javax.servlet.ServletContext

import altitude.controllers.{ImportServlet, AltitudeServlet}
import org.scalatra._

class ScalatraBootstrap extends LifeCycle {
  override def init(context: ServletContext) {
    context.mount(new AltitudeServlet, "/*")
    context.mount(new ImportServlet, "/import/*")
  }
}
