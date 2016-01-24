import javax.servlet.ServletContext

import altitude.{SingleApplication, Environment}
import altitude.controllers._
import altitude.controllers.api.{LibraryController, TagConfigController, SearchController, ImportProfileController}
import altitude.controllers.web.{StaticAssetController, IndexController, AssetController, SearchController}
import org.scalatra._
import org.slf4j.LoggerFactory

class ScalatraBootstrap extends LifeCycle with SingleApplication {
  private final val log = LoggerFactory.getLogger(getClass)

  override def init(context: ServletContext) {
    context.mount(new LibraryController, "/api/v1/library/*")
    context.mount(new web.IndexController, "/")
    context.mount(new web.ImportController, "/cl/import/*")
    context.mount(new web.SearchController, "/cl/search/*")
    context.mount(new web.UncategorizedController, "/cl/uncategorized")

    context.mount(new AssetController, "/assets/*")
    context.mount(new ImportController, "/import/*")

    context.mount(new StaticAssetController, "/static/*")

    context.mount(new api.SearchController, "/api/v1/search/*")
    context.mount(new api.FolderController, "/api/v1/folders/*")
    //context.mount(new api.ImportProfileController, "/api/v1/ip/*")
    //context.mount(new api.TagConfigController, "/api/v1/tagconfig/*")

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
