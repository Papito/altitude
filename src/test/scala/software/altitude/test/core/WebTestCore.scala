package software.altitude.test.core

import org.scalatra.test.ScalatraTests
import org.scalatra.test.scalatest.ScalatraFunSuite
import software.altitude.core.AltitudeServletContext

abstract class WebTestCore extends IntegrationTestCore with ScalatraTests with ScalatraFunSuite {

  override def header = null

  // mount all controllers, just as we do in ScalatraBootstrap
  AltitudeServletContext.endpoints.foreach { case (servlet, path) =>
    mount(servlet, path)
  }
}
