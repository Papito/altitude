package software.altitude.test.core.integration

import org.scalatest.Suites
import software.altitude.test.core.unit.{ModelTests, FolderModelTests, SearchQueryModelTests}

abstract class AllTests(val config: Map[String, Any]) extends Suites(
    new ModelTests,
    new SearchQueryModelTests,
    new AssetServiceTests(config),
    new MetadataParserTests(config),
    new MetadataServiceTests(config),
    new AssetQueryTests(config),
    new SearchServiceTests(config),
    new RepositoryServiceTests(config),
    new FileSystemImportTests(config),
    new FolderModelTests,
    new FolderServiceTests(config),
    new FileStoreServiceTests(config),
    new LibraryServiceTests(config),
    new StatsServiceTests(config)
)

