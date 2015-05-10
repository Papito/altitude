package integration

abstract class AllTests(config: Map[String, _]) extends Suites(
  new ImportTests(config)
)
