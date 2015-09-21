import org.eclipse.jetty.server.Server

import org.eclipse.jetty.server._
import org.eclipse.jetty.webapp.WebAppContext
import org.scalatra.servlet.ScalatraListener

object ScalatraLauncher extends App {

  // in development mode start with -DresourceBase=target/webapp
  val resourceBase = getClass.getClassLoader.getResource("webapp").toExternalForm
  val host = "localhost"
  val port = 8080

  val server = new Server
  server.setStopTimeout(5000)
  server.setStopAtShutdown(true)

  val connector = new ServerConnector(server)
  connector.setHost(host)
  connector.setPort(port)
  server.addConnector(connector)

  val context = new WebAppContext(getClass.getClassLoader.getResource("webapp").toExternalForm, "/")
  context.setInitParameter("org.eclipse.jetty.servlet.Default.dirAllowed", "false")
  context.setEventListeners(Array(new ScalatraListener))
  server.setHandler(context)

  server.start()
  server.join()
}