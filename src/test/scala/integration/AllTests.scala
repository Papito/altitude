package integration

import org.scalatest.Suites
import unit.{SearchQueryTests, ModelTests}

abstract class AllTests(val config: Map[String, String]) extends Suites(
/*
  new ModelTests,
  new SearchQueryTests,
  new FileSystemImportTests(config),
  new MetadataParserTests(config),
  new ImportProfileTests(config),
  new AssetServiceTests(config),
  new FolderServiceTests(config),
  new LibraryServiceTests(config),
*/
  new SearchTests(config)
)
