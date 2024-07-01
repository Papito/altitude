package software.altitude.test.core.controller

import org.scalatest.DoNotDiscover
import software.altitude.test.core.WebTestCore

@DoNotDiscover class IndexControllerTests extends WebTestCore {

  test("New installation goes to setup page") {
    get("/") {
      altitude.service.system.readMetadata.isInitialized should equal(false)
      status should equal(302)
      response.getHeader("Location") should endWith("/setup")
    }
  }
}
