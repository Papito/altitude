package software.altitude.test.core

import org.scalatra.test.ScalatraTests
import org.scalatra.test.scalatest.ScalatraFunSuite
import play.api.libs.json.{JsObject, Json}
import software.altitude.core.AltitudeServletContext

abstract class HtmxTestCore extends IntegrationTestCore with ScalatraTests with ScalatraFunSuite {

  override def header = null

  protected def getHeaders: Map[String, String] = Map("Content-Type" -> "application/json")

  // mount all controllers, just as we do in ScalatraBootstrap
  AltitudeServletContext.endpoints.foreach { case (servlet, path) =>
    mount(servlet, path)
  }

}
