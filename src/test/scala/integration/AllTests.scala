package integration

import org.scalatest.Suites
import unit.{FolderModelTests, SearchQueryModelTests, ModelTests}

abstract class AllTests(val config: Map[String, String]) extends Suites(
  new MetadataParserTests(config)/*,
  new FileSystemImportTests(config),
  new FolderModelTests,
  new ModelTests,
  new SearchQueryModelTests,
  new ImportProfileTests(config),
  new FolderServiceTests(config),
  new AssetServiceTests(config),
  new LibraryServiceTests(config),
  new SearchTests(config)*/
)
