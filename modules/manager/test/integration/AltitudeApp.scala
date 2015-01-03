package integration

import global.Altitude
import org.scalatest.FunSuite
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Second, Span}
import org.scalatestplus.play.ConfiguredApp

trait AltitudeApp extends FunSuite with ConfiguredApp with ScalaFutures {
  protected def altitude: Altitude = Altitude.getInstance(this.app)
  // async setup
  implicit val defaultPatience = PatienceConfig(timeout = Span(1, Second), interval = Span(5, Millis))
}
