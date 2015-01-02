package integration

import org.scalatest.{DoNotDiscover, Suites, FunSuite}
import org.scalatestplus.play.{ConfiguredApp, OneAppPerSuite}
import play.api.test.FakeApplication

class AllTests extends Suites(
  new MongoSuite
)

class MongoSuite extends Suites(
  new ImportTests
) with OneAppPerSuite {
  implicit override lazy val app = FakeApplication(
    additionalConfiguration = Map("db.dataSource" -> "mongo")
  )
}

//class PostgresTests extends AllTests(Map("db.dataSource" -> "postgres"))

