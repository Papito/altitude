package integration

import org.scalatest.Suites

abstract class AllTests(val config: Map[String, String]) extends Suites(
  new ImportProfileTests(config),
  new FileSystemImportTests(config),
  new SearchTests(config),
  new AssetServiceTests(config)
)
