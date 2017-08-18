import javax.servlet.ServletContext

import software.altitude.core.controllers._
import software.altitude.core.controllers.api.admin.{MetadataController => AdminMetadataController}
import software.altitude.core.controllers.api.{MetadataController, StatsController, TrashController}
import software.altitude.core.controllers.web.{AssetController, StaticAssetController}
import software.altitude.core.{Environment, SingleApplication}
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

    context.mount(new software.altitude.core.controllers.api.AssetController, "/api/v1/assets/*")
    context.mount(new api.QueryController, "/api/v1/query/*")
    context.mount(new api.FolderController, "/api/v1/folders/*")
    context.mount(new TrashController, "/api/v1/trash/*")
    context.mount(new StatsController, "/api/v1/stats/*")
    context.mount(new MetadataController, "/api/v1/metadata/*")

    context.mount(new AdminMetadataController, "/api/v1/admin/metadata/*")

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
