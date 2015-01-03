package integration

import org.scalatest.{DoNotDiscover, Suites}
import org.scalatestplus.play.OneAppPerSuite
import play.api.test.FakeApplication

@DoNotDiscover class AllTests extends Suites(
  new ImportTests
)
class MongoSuite extends AllTests with OneAppPerSuite {
  implicit override lazy val app = FakeApplication(
    additionalConfiguration = Map("datasource" -> "mongo")
  )
}

class PostgresSuite extends AllTests with OneAppPerSuite {
  implicit override lazy val app = FakeApplication(
    additionalConfiguration = Map("datasource" -> "postgres")
  )
}
