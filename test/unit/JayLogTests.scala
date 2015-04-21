package unit

import java.util.logging.Level

import altitude.Util.log
import altitude.{Const => C}
import org.scalatest.Matchers._
import org.scalatest._
import play.api.libs.json._

class JayLogTests extends FunSuite {
  val V = Map(
    "v1" -> "value 1",
    "v2" -> "value 2")

  test("with message") {
    val jsonText: String = log.getLogStmt(Level.INFO, "message")
    println(jsonText)
    jsonText should not be Nil
    val json: JsValue = Json.parse(jsonText)

    (json \ "msg").as[String] should equal ("message")
    json \ "msg" shouldBe a [JsString]
    (json \ "level").as[String] should equal (Level.INFO.toString)
    json \ "tags" shouldBe a [JsArray]
    (json \ "tags").as[List[String]] shouldBe empty
  }

  test("with message and values") {
    val jsonText: String = log.getLogStmt(Level.INFO, "message", V)
    jsonText should not be Nil
    val json: JsValue = Json.parse(jsonText)

    json \ "v1" should not be Nil
    (json \ "v1").as[String] should equal ("value 1")
    json \ "v2" should not be Nil
    (json \ "v2").as[String] should equal ("value 2")
  }

  test("with message and tags") {
    val jsonText: String = log.getLogStmt(Level.INFO, "message", t = Seq[String](C.tag.APP))
    jsonText should not be Nil
    val json: JsValue = Json.parse(jsonText)
    (json \ "tags").as[List[String]] should have size 1
    (json \ "tags").as[List[String]] should contain (C.tag.APP)
  }

  test("with message, tags, and values") {
    val jsonText: String = log.getLogStmt(Level.WARNING, "message", V, Seq[String](C.tag.WEB, C.tag.DB))
    jsonText should not be Nil
    val json: JsValue = Json.parse(jsonText)
    (json \ "level").as[String] should equal (Level.WARNING.toString)
    (json \ "tags").as[List[String]] should have size 2
    (json \ "tags").as[List[String]] should contain (C.tag.WEB)
    (json \ "tags").as[List[String]] should contain (C.tag.DB)

    json \ "v1" should not be Nil
    (json \ "v1").as[String] should equal ("value 1")
    json \ "v2" should not be Nil
    (json \ "v2").as[String] should equal ("value 2")
  }

  test("variable substitution") {
    val jsonText: String = log.getLogStmt(Level.INFO, "$v1, $v2", V)
    jsonText should not be Nil
    val json: JsValue = Json.parse(jsonText)

    json \ "v1" should not be Nil
    (json \ "v1").as[String] should equal ("value 1")
    json \ "v2" should not be Nil
    (json \ "v2").as[String] should equal ("value 2")

    (json \ "msg").as[String] should equal ("value 1, value 2")
  }

  test("name collision") {
    val jsonText: String = log.getLogStmt(
      Level.INFO, "message", Map("msg" -> "Success message")
    )

    val json: JsValue = Json.parse(jsonText)

    (json \ "msg").as[String] should equal ("Success message")
  }

  test("log constants") {
    log.addConstant("module", "JayLog")
    log.addConstant("threads", 4)

    val jsonText: String = log.getLogStmt(
      Level.INFO, "constants test"
    )

    val json: JsValue = Json.parse(jsonText)

    json \ "module" should not be Nil
    (json \ "module").as[String] should equal ("JayLog")
    json \ "threads" should not be Nil
    (json \ "threads").as[String] should equal ("4")
  }
}