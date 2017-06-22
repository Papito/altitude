import javax.servlet.ServletContext

import altitude.controllers._
import altitude.controllers.api.{StatsController, TrashController}
import altitude.controllers.web.{AssetController, StaticAssetController}
import altitude.{Environment, SingleApplication}
import org.scalatra._
import org.slf4j.LoggerFactory

class ScalatraBootstrap extends LifeCycle with SingleApplication {
  private final val log = LoggerFactory.getLogger(getClass)

  override def init(context: ServletContext) {
    context.mount(new web.IndexController, "/")
    context.mount(new web.ClientController, "/client/*")

    context.mount(new AssetController, "/assets/*")
    context.mount(new ImportController, "/import/*")

    context.mount(new StaticAssetController, "/static/*")

    context.mount(new altitude.controllers.api.AssetController, "/api/v1/assets/*")
    context.mount(new api.QueryController, "/api/v1/query/*")
    context.mount(new api.FolderController, "/api/v1/folders/*")
    context.mount(new TrashController, "/api/v1/trash/*")
    context.mount(new StatsController, "/api/v1/stats/*")

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
    log.info("All done.")
  }
}
