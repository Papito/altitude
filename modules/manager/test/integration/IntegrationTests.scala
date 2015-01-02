package integration

import org.scalatest.FunSuite
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Second, Span, Millis}
import org.scalatestplus.play.OneAppPerSuite

trait IntegrationTests extends FunSuite with OneAppPerSuite with ScalaFutures {
  implicit val defaultPatience = PatienceConfig(timeout = Span(1, Second), interval = Span(5, Millis))
}
