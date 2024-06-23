package software.altitude.test.core.suites

import org.scalatest.Suites
import software.altitude.test.core.unit.ApiValidatorTests
import software.altitude.test.core.unit.FolderModelTests
import software.altitude.test.core.unit.ModelTests
import software.altitude.test.core.unit.SearchSqlQueryTests
import software.altitude.test.core.unit.SqlQueryTests

class UnitTestSuites extends Suites (
  new ModelTests,
  new FolderModelTests,
  new SqlQueryTests,
  new SearchSqlQueryTests,
  new ApiValidatorTests,
)
