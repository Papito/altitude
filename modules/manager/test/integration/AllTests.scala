package integration

import org.scalatest.{DoNotDiscover, Suites}

@DoNotDiscover class AllTests extends Suites(
  new ImportTests
)
