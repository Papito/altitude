import org.eclipse.jetty.server._
import org.eclipse.jetty.servlet.DefaultServlet
import org.eclipse.jetty.webapp.WebAppContext
import org.scalatra.servlet.ScalatraListener
import software.altitude.core.AltitudeServletContext

object ScalatraLauncher extends App with AltitudeServletContext {
  private val port = 9010

  private val server = new Server(port)
  server.setStopTimeout(5000)
  server.setStopAtShutdown(true)

  val context = new WebAppContext()
  context setContextPath "/"
  context.setResourceBase("src/main/webapp")
  context.addEventListener(new ScalatraListener)
  context.addServlet(classOf[DefaultServlet], "/")

  server.setHandler(context)

  server.start()
  server.join()
}
