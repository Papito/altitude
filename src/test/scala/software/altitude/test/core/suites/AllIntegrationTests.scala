package software.altitude.test.core.suites

import org.scalatest.Suites
import software.altitude.test.core.api.AssetApiControllerTests
import software.altitude.test.core.integration._
import software.altitude.test.core.unit._
import software.altitude.test.core.web.IndexControllerTests

abstract class AllIntegrationTests(val config: Map[String, Any]) extends Suites (
  // INTEGRATION (services)
  new AssetQueryTests(config),
  new SearchQueryModelTests,
  new AssetServiceTests(config),
  new MetadataParserTests(config),
  new SearchServiceTests(config),
  new RepositoryServiceTests(config),
  new ImportTests(config),
  new FolderServiceTests(config),
  new FileStoreServiceTests(config),
  new StatsServiceTests(config),
  new UserServiceTests(config),
  new LibraryServiceTests(config),
  new MetadataServiceTests(config),

  // HTTP:API
  new AssetApiControllerTests(config),

  // HTTP:WEB
  new IndexControllerTests(config)
)
