import javax.servlet.ServletContext

import altitude._
import org.scalatra._

class ScalatraBootstrap extends LifeCycle {
  override def init(context: ServletContext) {
    context.mount(new AltitudeServlet, "/*")
  }
}
