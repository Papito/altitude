package software.altitude.test.core.unit

import org.scalatest._
import play.api.libs.json._
import software.altitude.core.models.BaseModel
import software.altitude.test.core.TestFocus

class ModelTests extends FunSuite with TestFocus {
  case class TestModel(id: Option[String] = None,
                       stringProp: String,
                       boolProp: Boolean,
                       intProp: Int) extends BaseModel {
    val toJson: JsObject = coreJsonAttrs
  }

  test("Create a model", focused) {
    TestModel(stringProp = "stringProp", boolProp = true, intProp = 2)
  }
}