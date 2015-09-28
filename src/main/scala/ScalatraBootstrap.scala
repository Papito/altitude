import javax.servlet.ServletContext

import altitude.{SingleApplication, Environment}
import altitude.controllers._
import altitude.controllers.api.{TagConfigController, SearchController, ImportProfileController}
import altitude.controllers.web.{IndexController, ImportController, AssetController}
import org.scalatra._
import org.slf4j.LoggerFactory

class ScalatraBootstrap extends LifeCycle with SingleApplication {
  private final val log = LoggerFactory.getLogger(getClass)

  override def init(context: ServletContext) {
    context.mount(new IndexController, "/*")
    context.mount(new ImportController, "/import/*")
    context.mount(new AssetController, "/assets/*")
    context.mount(new SearchController, "/api/search/*")
    context.mount(new ImportProfileController, "/api/ip/*")
    context.mount(new TagConfigController, "/api/tagconfig/*")

    context.initParameters("org.scalatra.environment") = Environment.ENV match {
      case Environment.DEV => "development"
      case Environment.PROD => "production"
    }
  }

  override def destroy(context: ServletContext) {
    log.info("Cleaning up after ourselves...")
    app.freeResources()
    super.destroy(context)
    log.info("All done. Come back soon.")
  }
}
