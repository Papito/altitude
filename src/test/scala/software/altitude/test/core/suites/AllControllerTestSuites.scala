package software.altitude.test.core.suites

import org.scalatest.Suites
import software.altitude.test.core.controller.FileUploadControllerTests
import software.altitude.test.core.controller.IndexControllerTests
import software.altitude.test.core.controller.SetupControllerTests

abstract class AllControllerTestSuites extends Suites (
  new SetupControllerTests,
  new IndexControllerTests,
  new FileUploadControllerTests
)
