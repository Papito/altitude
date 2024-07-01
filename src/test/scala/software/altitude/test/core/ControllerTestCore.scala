package software.altitude.test.core

import org.scalatest.BeforeAndAfter
import org.scalatest.BeforeAndAfterEach
import org.scalatest.funsuite
import org.scalatra.test.ScalatraTests
import org.scalatra.test.scalatest.ScalatraFunSuite
import software.altitude.core.AltitudeServletContext
import software.altitude.core.Const
import software.altitude.core.Environment
import software.altitude.core.models.Repository
import software.altitude.core.models.User
import software.altitude.test.core.integration.TestContext
import software.altitude.test.core.suites.PostgresSuiteBundle

abstract class ControllerTestCore
  extends funsuite.AnyFunSuite
  with ScalatraTests
  with ScalatraFunSuite
  with BeforeAndAfter
  with BeforeAndAfterEach
  with testAltitudeApp
  with TestFocus {

  // mount all controllers, just as we do in ScalatraBootstrap
  AltitudeServletContext.endpoints.foreach { case (servlet, path) =>
    mount(servlet, path)
  }

  def testAuthHeaders(user: Option[User] = None, repo: Option[Repository] = None): List[(String, String)] = {
    val userHeader = if (user.isEmpty) {
      Const.Api.USER_TEST_HEADER_ID -> testContext.user.persistedId
    } else {
      Const.Api.USER_TEST_HEADER_ID -> user.get.persistedId
    }

    val repoHeader = if (repo.isEmpty) {
      Const.Api.REPO_TEST_HEADER_ID -> testContext.repository.persistedId
    } else {
      Const.Api.REPO_TEST_HEADER_ID -> repo.get.persistedId
    }

    List(userHeader, repoHeader)
  }

  Environment.ENV = Environment.TEST

  override def beforeEach(): Unit = {
    // the database is dirtied by the separate process (test server)
    // so we need to reset it before each test
    PostgresSuiteBundle.setup(testApp)
    IntegrationTestCore.createTestDir(testApp.app)

    // the few application state variables should also be rolled back
    AltitudeServletContext.app.isInitialized = false
  }

  var testContext: TestContext = new TestContext(testApp)

  // I have no idea what this is for
  override def header = null
}
