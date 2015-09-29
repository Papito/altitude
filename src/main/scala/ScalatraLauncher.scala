import altitude.SingleApplication
import org.eclipse.jetty.server.Server

import org.eclipse.jetty.server._
import org.eclipse.jetty.servlet.DefaultServlet
import org.eclipse.jetty.webapp.WebAppContext
import org.scalatra.servlet.ScalatraListener
import org.slf4j.LoggerFactory

object ScalatraLauncher extends App  with SingleApplication {
  private final val log = LoggerFactory.getLogger(getClass)

  // in development mode start with -DresourceBase=target/webapp
  val host = "localhost"
  val port = 8080

  val server = new Server
  server.setStopTimeout(5000)
  server.setStopAtShutdown(true)

  val connector = new ServerConnector(server)
  connector.setHost(host)
  connector.setPort(port)
  server.addConnector(connector)

  val r = getClass.getResource("logback.xml").toExternalForm
  val pattern = """jar:file:(.*)/lib/altitude.*""".r
  val pattern(path) = r
  log.info(path)

/*
  val context = new WebAppContext()
  context setContextPath "/"
  context.setResourceBase(path)
  context.addEventListener(new ScalatraListener)
  context.addServlet(classOf[DefaultServlet], "/")
*/

  val context = new WebAppContext(path + "/client", "/")
  context.setInitParameter("org.eclipse.jetty.servlet.Default.dirAllowed", "false")
  context.setEventListeners(Array(new ScalatraListener))

  server.setHandler(context)
  
  server.start()
  server.join()
}