package integration

import org.scalatest.Suites
import unit.{FolderModelTests, SearchQueryModelTests, ModelTests}

abstract class AllTests(val config: Map[String, String]) extends Suites(
  new FileSystemImportTests(config),
  new MetadataParserTests(config),
  new FolderModelTests,
  new ModelTests,
  new SearchQueryModelTests,
  new ImportProfileTests(config),
  new SearchTests(config),
  new FolderServiceTests(config),
  new AssetServiceTests(config),
  new LibraryServiceTests(config),
  new StatsServiceTests(config)
)
