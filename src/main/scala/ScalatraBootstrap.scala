import javax.servlet.ServletContext

import altitude.Environment
import altitude.controllers.{SearchApiController, IndexController, AssetController, ImportController}
import org.scalatra._
import org.slf4j.LoggerFactory

class ScalatraBootstrap extends LifeCycle {
  val log =  LoggerFactory.getLogger(getClass)

  override def init(context: ServletContext) {
    context.mount(new IndexController, "/*")
    context.mount(new ImportController, "/import/*")
    context.mount(new AssetController, "/assets/*")
    context.mount(new SearchApiController, "/api/search/*")

    val environment = Environment.ENV match {
      case Environment.DEV => "development"
      case Environment.PROD => "production"
    }

    context.initParameters("org.scalatra.environment") = environment
  }
}
