package software.altitude.core

import org.scalatra.servlet.ServletApiImplicits._
import org.slf4j.LoggerFactory
import software.altitude.core.controllers.api._
import software.altitude.core.controllers.web.SessionController
import software.altitude.core.controllers.web.WebIndexController

import javax.servlet.ServletContext

object AltitudeServletContext {
  private final val log = LoggerFactory.getLogger(getClass)
  log.info("Initializing application context... ")
  val app: Altitude = new Altitude

  // private val actorSystem = ActorSystem()

  def mountEndpoints(context: ServletContext): Unit = {
    context.mount(new WebIndexController, "/*")

    context.mount(new AssetController, "/api/v1/assets/*")
    context.mount(new SearchController, "/api/v1/search/*")
    context.mount(new FolderController, "/api/v1/folders/*")
    context.mount(new TrashController, "/api/v1/trash/*")
    context.mount(new StatsController, "/api/v1/stats/*")
    context.mount(new MetadataController, "/api/v1/metadata/*")
    context.mount(new SessionController, "/sessions/*")
    context.mount(new admin.MetadataController, "/api/v1/admin/metadata/*")
//    context.mount(new FileSystemBrowserController, "/navigate/*")
    // context.mount(new ImportController(actorSystem), "/import/*")
  }
}

trait AltitudeServletContext {
  val app: Altitude = AltitudeServletContext.app
}
