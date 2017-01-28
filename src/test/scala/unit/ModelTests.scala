package unit

import altitude.models.BaseModel
import altitude.{Const => C}
import org.scalatest._
import play.api.libs.json._

class ModelTests extends FunSuite {
  case class TestModel(id: Option[String] = None) extends BaseModel {
    val toJson: JsObject = coreJsonAttrs
  }

  test("create a model") {
    TestModel()
  }
}