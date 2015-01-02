package integration

import com.google.inject.Guice
import global.ManagerGlobal
import org.scalatest.{DoNotDiscover, Suites, FunSuite}
import org.scalatestplus.play.{ConfiguredApp, OneAppPerSuite}
import play.api.test.FakeApplication

@DoNotDiscover class AllTests extends Suites(
  new ImportTests
)

class MongoSuite extends AllTests with OneAppPerSuite {
  implicit override lazy val app = FakeApplication(
    additionalConfiguration = Map("db.dataSource" -> "mongo")
  )
}

class PostgresSuite extends AllTests with OneAppPerSuite {
  implicit override lazy val app = FakeApplication(
    additionalConfiguration = Map("db.dataSource" -> "postgres")
  )
}

