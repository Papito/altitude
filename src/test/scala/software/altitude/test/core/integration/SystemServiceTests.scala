package software.altitude.test.core.integration

import org.scalatest.DoNotDiscover
import org.scalatest.matchers.must.Matchers.be
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import software.altitude.test.core.IntegrationTestCore

@DoNotDiscover class SystemServiceTests(val config: Map[String, Any]) extends IntegrationTestCore {

  test("Initialize system") {
    val userModel = testContext.makeUser()

    altitude.service.system.initializeSystem(
      repositoryName = "My Repository",
      adminModel=userModel,
      password = "password3000")

    val systemMetadata = altitude.service.system.readMetadata

    systemMetadata.isInitialized should be(true)
    altitude.isInitialized should be(true)
  }
}
