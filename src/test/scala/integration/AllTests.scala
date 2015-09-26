package integration

import org.scalatest.Suites

abstract class AllTests(val config: Map[String, String]) extends Suites(
  new MetadataParserTests(config)/*,
  new FileSystemImportTests(config),
  new ImportProfileTests(config),
  new SearchTests(config),
  new AssetServiceTests(config)*/
)
