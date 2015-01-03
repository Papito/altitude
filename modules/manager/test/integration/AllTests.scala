package integration

import global.BaseManagerGlobal
import org.scalatest.{DoNotDiscover, Suites}
import org.scalatestplus.play.OneAppPerSuite
import play.api.test.FakeApplication

@DoNotDiscover class AllTests extends Suites(
  new ImportTests
)

class MongoSuite extends AllTests with OneAppPerSuite {
  override lazy val app = FakeApplication(
    additionalConfiguration = Map("datasource" -> "mongo"),
    withGlobal = Some(new BaseManagerGlobal())
  )
}

class PostgresSuite extends AllTests with OneAppPerSuite {
  override lazy val app = FakeApplication(
    additionalConfiguration = Map("datasource" -> "postgres"),
      withGlobal = Some(new BaseManagerGlobal())
  )
}
