import javax.servlet.ServletContext
import org.scalatra._
import org.slf4j.{Logger, LoggerFactory}
import software.altitude.core.AltitudeServletContext
import software.altitude.core.Environment

class ScalatraBootstrap extends LifeCycle with AltitudeServletContext {
  val logger: Logger = LoggerFactory.getLogger(getClass)


  override def init(context: javax.servlet.ServletContext): Unit = {
    super.init(context)

    sys.addShutdownHook {
      destroy(context)
    }

    context.setInitParameter("org.scalatra.environment", Environment.CURRENT)

    AltitudeServletContext.mountEndpoints(context)
    AltitudeServletContext.app.runMigrations()
    AltitudeServletContext.app.setIsInitializedState()

    AltitudeServletContext.app.service.faceCache.loadCacheForAll()
    AltitudeServletContext.app.service.faceRecognition.initialize()
  }

  override def destroy(context: ServletContext): Unit = {
    logger.warn("Shutting down application")
    AltitudeServletContext.app.cleanup()
  }
}
