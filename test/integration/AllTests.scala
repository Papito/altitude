package integration

import org.scalatest.Suites

abstract class AllTests(config: Map[String, _]) extends Suites(
  new ImportTests(config),
  new BasePostgresDaoTests(config),
  new BaseMongoDaoTests(config)
)
