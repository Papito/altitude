import java.io.File
import java.net.{URL, URISyntaxException}

import altitude.{Environment, SingleApplication}
import org.eclipse.jetty.server.Server

import org.eclipse.jetty.server._
import org.eclipse.jetty.server.handler.ContextHandlerCollection
import org.eclipse.jetty.servlet.{DefaultServlet, ServletContextHandler, ServletHolder}
import org.eclipse.jetty.webapp.WebAppContext
import org.scalatra.servlet.ScalatraListener
import org.slf4j.LoggerFactory

object ScalatraLauncher extends App  with SingleApplication {
  private final val log = LoggerFactory.getLogger(getClass)

  val host = "localhost"
  val port = 8080

  val server = new Server
  server.setStopTimeout(5000)
  server.setStopAtShutdown(true)

  val connector = new ServerConnector(server)
  connector.setHost(host)
  connector.setPort(port)
  server.addConnector(connector)

  val staticContext = new WebAppContext(Environment.root + "static", "/static/")
  staticContext.setInitParameter("org.eclipse.jetty.servlet.Default.dirAllowed", "false")
  staticContext.addServlet(classOf[DefaultServlet],"/")

  val context = new WebAppContext(getClass.getClassLoader.getResource("webapp").toExternalForm, "/")
  context.setEventListeners(Array(new ScalatraListener))

  val contexts = new ContextHandlerCollection()
  contexts.setHandlers(List[Handler](staticContext, context).toArray)

  server.setHandler(contexts)

  server.start()
  server.join()
}