package unit

import org.scalatest._
import play.api.libs.json._
import software.altitude.core.models.BaseModel
import software.altitude.core.{Const => C}

class ModelTests extends FunSuite {
  case class TestModel(id: Option[String] = None) extends BaseModel {
    val toJson: JsObject = coreJsonAttrs
  }

  test("create a model") {
    TestModel()
  }
}