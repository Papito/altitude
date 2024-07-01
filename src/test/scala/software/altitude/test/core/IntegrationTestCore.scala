package software.altitude.test.core

import org.apache.commons.io.FileUtils
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import org.scalatest._
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import software.altitude.core._
import software.altitude.core.models._
import software.altitude.test.core.integration.TestContext

import java.io.File
import scala.language.implicitConversions


object IntegrationTestCore {
  // TODO: move into test utils
  def createTestDir(testApp: Altitude): Unit = {
    val testDir = new File(testApp.config.getString("testDir"))

    if (!testDir.exists()) {
      FileUtils.forceMkdir(testDir)
    }
  }

  // TODO: move into test utils
  def createFileStoreDir(altitude: AltitudeAppContext): Unit = {
    val dataDir = new File(altitude.config.getString("dataDir"))

    if (dataDir.exists()) {
      FileUtils.cleanDirectory(dataDir)
    }
    else {
      FileUtils.forceMkdir(dataDir)
    }
  }

  /**
   * Convert a file system resource to an import asset
   */
  // TODO: move into test utils (may not need this at all, actually as redundant)
  def fileToImportAsset(file: File): ImportAsset = new ImportAsset(
    fileName = file.getName,
    data = FileUtils.readFileToByteArray(file),
    metadata = Metadata())
}

/**
 * The trait provides utility methods for creating Mockito `Answer` instances.
 * These methods simplify the creation of `Answer` instances by allowing the use of lambda expressions.
 */
// FIXME: also test utils
trait AnswerSugar {
  /**
   * Converts a function with no arguments to a Mockito `Answer`.
   * This method is used when creating a Mockito `Answer` that does not require any information from the `InvocationOnMock`.
   *
   * @param f A function that takes no arguments and returns a value of type `T`.
   * @return An `Answer[T]` that, when invoked, calls the provided function `f`.
   */
  implicit def toAnswer[T](f: () => T): Answer[T] = new Answer[T] {
    override def answer(invocation: InvocationOnMock): T = f()
  }

  /**
   * Converts a function with an `InvocationOnMock` argument to a Mockito `Answer`.
   * This method is used when creating a Mockito `Answer` that requires information from the `InvocationOnMock`.
   *
   * @param f A function that takes an `InvocationOnMock` and returns a value of type `T`.
   * @return An `Answer[T]` that, when invoked, calls the provided function `f` with the `InvocationOnMock`.
   */
  implicit def toAnswerWithArguments[T](f: InvocationOnMock => T): Answer[T] = new Answer[T] {
    override def answer(invocation: InvocationOnMock): T = f(invocation)
  }
}

abstract class IntegrationTestCore
    extends funsuite.AnyFunSuite
    with testAltitudeApp
    with BeforeAndAfter
    with BeforeAndAfterEach
    with OptionValues
    with AnswerSugar
    with TestFocus {
  protected final val log: Logger = LoggerFactory.getLogger(getClass)

  // Force environment to always be TEST
  Environment.ENV = Environment.TEST

  override def beforeEach(): Unit = {
    AltitudeServletContext.app.isInitialized = false
    testContext = new TestContext(testApp)

    /*
     Every integration test has at least one repository to start with - you can't do anything otherwise.
     This also creates the first user (owner of the repository).

     Tests then can create additional repos and users to test the boundaries of repository and user separation.
     */
    testContext.persistRepository()

    IntegrationTestCore.createFileStoreDir(testApp)
  }

  override def afterEach(): Unit = {
    // We COULD rollback here, but we don't need to, and committing is better for checking repo/user isolation
    testApp.txManager.commit()
  }

  var testContext: TestContext = new TestContext(testApp)

  def savepoint(): Unit = {
    testApp.txManager.savepoint()
  }

  def switchContextUser(user: User): Unit = {
    testApp.service.user.switchContextToUser(user)
  }

  def switchContextRepo(repository: Repository): Unit = {
    testApp.service.repository.switchContextToRepository(repository)
  }

  def reset(): Unit = {
    testApp.txManager.rollback()
    RequestContext.clear()
  }
}
