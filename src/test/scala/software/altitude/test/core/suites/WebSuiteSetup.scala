package software.altitude.test.core.suites

import org.scalatest.BeforeAndAfterAll
import org.scalatest.Suite
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import software.altitude.core.Environment
import software.altitude.core.RequestContext

trait WebSuiteSetup extends Suite with BeforeAndAfterAll {
  Environment.ENV = Environment.TEST
  protected final val log: Logger = LoggerFactory.getLogger(getClass)

  override def beforeAll(): Unit = {
    println("\n@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@")
    println("WEB CONTROLLER INTEGRATION TESTS")
    println("@@@@@@@@@@@@@@@@@@@@@@@@#@@@@@@@\n")

    // we are testing HTTP server output doing its own thing in a different process, so we cannot
    // and should not write to anything - the connection here is just to explore the state of the DB
    RequestContext.conn.value = Some(PostgresSuite.app.txManager.connection(readOnly = true))
  }

  override def afterAll(): Unit = {
    PostgresSuite.app.txManager.close()
  }
}
