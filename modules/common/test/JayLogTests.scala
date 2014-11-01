import org.scalatest._
import util.log

class JayLogTests extends FunSuite {

  test("Logger interface") {
    val v = Map(
      "v1" -> "v1",
      "v2" -> "v2")

    log.info("message")
    log.info("message", v)
    log.info("message", v, log.WEB, log.STORAGE)
    log.info("message", log.APP)
  }
}