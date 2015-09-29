import java.io.File
import java.net.{URL, URISyntaxException}

import altitude.SingleApplication
import org.eclipse.jetty.server.Server

import org.eclipse.jetty.server._
import org.eclipse.jetty.webapp.WebAppContext
import org.scalatra.servlet.ScalatraListener
import org.slf4j.LoggerFactory

object ScalatraLauncher extends App  with SingleApplication {
  private final val log = LoggerFactory.getLogger(getClass)

  var jarPath: Option[File] = None

  try {
    val url = ScalatraLauncher.getClass.getProtectionDomain.getCodeSource.getLocation
    jarPath = Some(new File(url.toURI).getParentFile)
  } catch {
    case ex: URISyntaxException => log.error("jarPath ERROR: " + ex.getMessage)
  }

  val host = "localhost"
  val port = 8080

  val server = new Server
  server.setStopTimeout(5000)
  server.setStopAtShutdown(true)

  val connector = new ServerConnector(server)
  connector.setHost(host)
  connector.setPort(port)
  server.addConnector(connector)

  val context = new WebAppContext(jarPath.get.getPath + File.separator + ".." + File.separator + "client", "/")
  context.setInitParameter("org.eclipse.jetty.servlet.Default.dirAllowed", "false")
  context.setEventListeners(Array(new ScalatraListener))

  server.setHandler(context)

  server.start()
  server.join()
}