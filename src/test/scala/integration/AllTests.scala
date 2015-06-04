package integration

import org.scalatest.Suites

abstract class AllTests(config: Map[String, String]) extends Suites(
  new ImportTests(config)
)
