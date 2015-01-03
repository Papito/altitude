package integration

import global.Altitude
import org.scalatest.FunSuite
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Second, Span}
import org.scalatestplus.play.OneAppPerSuite
import play.api.test.FakeApplication

abstract class IntegrationTestCore extends FunSuite with OneAppPerSuite with ScalaFutures {
  val config: Map[String, _]

  override lazy val app = FakeApplication(
    additionalConfiguration = config
  )

  protected lazy val altitude: Altitude = Altitude.getInstance(this.app)

  // async setup
  implicit val defaultPatience = PatienceConfig(timeout = Span(1, Second), interval = Span(5, Millis))
}
