package integration

import org.scalatest.Suites

abstract class AllTests(val config: Map[String, String]) extends Suites(
  new FileSystemImportTests(config),
  new MetadataParserTests(config),
  new ImportProfileTests(config),
  new SearchTests(config),
  new AssetServiceTests(config),
  new FolderTests(config)
)
