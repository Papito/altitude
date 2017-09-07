import javax.servlet._

import org.scalatra._
import org.slf4j.LoggerFactory
import software.altitude.core.{Environment, SingleApplication}

class ScalatraBootstrap extends LifeCycle with SingleApplication {
  private final val log = LoggerFactory.getLogger(getClass)

  override def init(context: ServletContext) {
    context.initParameters("org.scalatra.environment") = Environment.ENV match {
      case Environment.DEV => "development"
      case Environment.PROD => "production"
    }

    SingleApplication.mountEndpoints(context)

    SingleApplication.app.runMigrations()
  }

  override def destroy(context: ServletContext) {
    log.info("Cleaning up after ourselves...")
    app.freeResources()
    super.destroy(context)
    log.info("All done.")
  }
}
