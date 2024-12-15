import javax.servlet.ServletContext
import org.scalatra._

import software.altitude.core.AltitudeServletContext
import software.altitude.core.Environment

class ScalatraBootstrap extends LifeCycle with AltitudeServletContext {

  override def init(context: javax.servlet.ServletContext): Unit = {

    context.setInitParameter("org.scalatra.environment", Environment.CURRENT)

    AltitudeServletContext.mountEndpoints(context)
    AltitudeServletContext.app.runMigrations()
    AltitudeServletContext.app.setIsInitializedState()

    AltitudeServletContext.app.service.faceCache.loadCacheForAll()
    AltitudeServletContext.app.service.faceRecognition.initialize()
  }

  override def destroy(context: ServletContext): Unit = {
    AltitudeServletContext.app.cleanup()
    super.destroy(context)
  }
}
