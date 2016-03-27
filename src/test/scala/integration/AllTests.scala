package integration

import org.scalatest.Suites

abstract class AllTests(val config: Map[String, String]) extends Suites(
/*
  new FileSystemImportTests(config),
  new MetadataParserTests(config),
  new ImportProfileTests(config),
  new AssetServiceTests(config),
  new FolderServiceTests(config),
  new LibraryServiceTests(config),
*/
  new SearchTests(config)
)
