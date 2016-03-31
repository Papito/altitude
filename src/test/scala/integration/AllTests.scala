package integration

import org.scalatest.Suites
import unit.{SearchQueryTests, ModelTests}

abstract class AllTests(val config: Map[String, String]) extends Suites(
/*
  new ModelTests,
  new SearchQueryTests,
  new MetadataParserTests(config),
  new ImportProfileTests(config),
  new FolderServiceTests(config),
*/
  new AssetServiceTests(config),
  new LibraryServiceTests(config),
  new FileSystemImportTests(config),
  new SearchTests(config)
)
