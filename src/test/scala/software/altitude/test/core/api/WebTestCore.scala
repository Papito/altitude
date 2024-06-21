package software.altitude.test.core.api

import org.scalatra.test.scalatest.ScalatraFunSuite
import software.altitude.core.controllers.web.WebIndexController
import software.altitude.test.core.integration.IntegrationTestCore

abstract class WebTestCore extends IntegrationTestCore with ScalatraFunSuite {
  override def header = null
  addServlet(classOf[WebIndexController], "/*")
}
