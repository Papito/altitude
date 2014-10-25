import org.scalatest._
import util.log

class JayLogTests extends FunSuite {

  test("An empty Set should have size 0") {
    val v = Map[String, String]("v1" -> "v1", "v2" -> "v2")
    val t = Set[String]("tag1", "tag2")

    log.info("message")
    log.info("message", v)
    log.info("message", v, "tag1", "tag2")
    log.info("message", "tag1", "tag2")
  }
}