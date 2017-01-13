package integration

import org.scalatest.Suites
import unit.{FolderModelTests, ModelTests, SearchQueryModelTests}

abstract class AllTests(val config: Map[String, String]) extends Suites(
/*
    new FolderModelTests,
    new ModelTests,
    new SearchQueryModelTests,
    new SearchTests(config),
    new AssetServiceTests(config),
    new RepositoryTests(config),
    new FolderServiceTests(config),
    new StatsServiceTests(config),
*/
    new FileSystemImportTests(config)
/*
    new MetadataParserTests(config),
    new LibraryServiceTests(config),
    new MetadataServiceTests(config),
    new SearchServiceTests(config),
    new FileSystemImportTests(config)
*/
)

