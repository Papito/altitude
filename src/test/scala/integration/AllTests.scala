package integration

import org.scalatest.Suites
import unit.{FolderModelTests, ModelTests, SearchQueryModelTests}

abstract class AllTests(val config: Map[String, Any]) extends Suites(
/*
    new FolderModelTests,
    new ModelTests,
    new SearchQueryModelTests,
    new AssetServiceTests(config),
    new FolderServiceTests(config),
    new MetadataParserTests(config),
    new MetadataServiceTests(config),
    new StatsServiceTests(config),
    new AssetQueryTests(config),
    new LibraryServiceTests(config),
    new SearchServiceTests(config),
    new FileSystemImportTests(config),
    new RepositoryServiceTests(config),
*/
    new FileStoreServiceTests(config)
)

