package integration

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Second, Span, Millis}

trait AsyncTestable extends ScalaFutures {
  implicit val defaultPatience = PatienceConfig(timeout = Span(1, Second), interval = Span(5, Millis))
}
