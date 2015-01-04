package integration

import global.manager.Altitude
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.{BeforeAndAfter, FunSuite}
import org.scalatestplus.play.OneAppPerSuite
import play.api.test.FakeApplication

abstract class IntegrationTestCore extends FunSuite
  with OneAppPerSuite with ScalaFutures with BeforeAndAfter {
  /* Stores test app config overrides, since we run same tests with different app setup.
   */
  val config: Map[String, _]

  before {
    altitude.service.dbUtilities.dropDatabase()
  }

  after {
  }

  override lazy val app = FakeApplication(
    additionalConfiguration = config
  )

  protected lazy val altitude: Altitude = Altitude.getInstance(this.app)

  // async setup
  implicit val defaultPatience = PatienceConfig(timeout = Span(2, Seconds), interval = Span(5, Millis))
}
