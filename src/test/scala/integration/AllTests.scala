package integration

import org.scalatest.Suites
import unit.{FolderModelTests, ModelTests, SearchQueryModelTests}

abstract class AllTests(val config: Map[String, Any]) extends Suites(
    new ModelTests,
    new SearchQueryModelTests,
    new AssetServiceTests(config),
    new MetadataParserTests(config),
    new MetadataServiceTests(config),
    new StatsServiceTests(config),
    new AssetQueryTests(config),
    new SearchServiceTests(config),
    new FileSystemImportTests(config),
    new RepositoryServiceTests(config),
    new FolderModelTests,
    new FolderServiceTests(config),
    new FileStoreServiceTests(config),
    new LibraryServiceTests(config)
)

