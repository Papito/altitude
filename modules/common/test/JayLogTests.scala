import org.scalatest._
import util.log
import constants.{const => C}

class JayLogTests extends FunSuite {

  test("Logger interface") {
    val v = Map(
      "v1" -> "v1",
      "v2" -> "v2")

    log.info("message")
    log.info("message", v)
    log.info("message", v, C.tag.WEB, C.tag.STORAGE)
    log.info("message", C.tag.APP)
  }
}