package software.altitude.test.core.suites

import org.scalatest.Suites
import software.altitude.test.core.integration._
import software.altitude.test.core.unit.{FolderModelTests, ModelTests, SearchQueryModelTests}

abstract class AllTests(val config: Map[String, Any]) extends Suites(
    new ModelTests,
    new SearchQueryModelTests,
    new AssetServiceTests(config),
    new MetadataParserTests(config),
    new AssetQueryTests(config),
    new SearchServiceTests(config),
    new RepositoryServiceTests(config),
    new ImportTests(config),
    new FolderModelTests,
    new FolderServiceTests(config),
    new FileStoreServiceTests(config),
    new LibraryServiceTests(config),
    new StatsServiceTests(config),
    new UserServiceTests(config),
    new MetadataServiceTests(config)
)

