package software.altitude.test.core.suites

import org.scalatest.Suites
import software.altitude.test.core.unit.{ApiValidatorTests, DataScrubberTests, FolderModelTests, ModelTests, SearchSqlQueryTests, SqlQueryTests}

abstract class AllUnitTests extends Suites (
  new ModelTests,
  new FolderModelTests,
  new SqlQueryTests,
  new SearchSqlQueryTests,
  new ApiValidatorTests,
  new DataScrubberTests,
)
