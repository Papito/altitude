package integration

import org.scalatestplus.play.OneAppPerSuite
import play.api.test.FakeApplication

class MongoSuite extends AllTests with OneAppPerSuite {
  override lazy val app = FakeApplication(
    additionalConfiguration = Map("datasource" -> "mongo")
  )
}
