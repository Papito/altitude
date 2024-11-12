package software.altitude.test.core.unit

import org.scalatest.DoNotDiscover
import org.scalatest.funsuite
import org.scalatest.matchers.must.Matchers.{be, include}
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import play.api.libs.json.JsonNaming.SnakeCase
import play.api.libs.json._
import software.altitude.core.models.BaseModel
import software.altitude.test.core.TestFocus

import scala.language.implicitConversions


@DoNotDiscover class CoreModelTests extends funsuite.AnyFunSuite with TestFocus {

  object TestModel {
    implicit val config: JsonConfiguration = JsonConfiguration(SnakeCase)
    implicit val writes: OWrites[TestModel] = Json.writes[TestModel]
    implicit val reads: Reads[TestModel] = Json.reads[TestModel]

    implicit def fromJson(json: JsValue): TestModel = Json.fromJson[TestModel](json).get
  }

  case class TestModel(id: Option[String] = None,
                       stringProp: String,
                       boolProp: Boolean,
                       intProp: Int) extends BaseModel {

    val toJson: JsObject = Json.toJson(this).as[JsObject] ++ coreJsonAttrs
  }

  test("Create a model") {
    TestModel(stringProp = "stringPropValue", boolProp = true, intProp = 2)
  }

  test("Model JSON conversion", Focused) {
    val obj = TestModel(stringProp = "stringPropValue", boolProp = true, intProp = 2)

    val jsonObj = Json.toJson(obj)
    jsonObj.toString() should include("string_prop\":\"stringPropValue\"")

    val objFromJson =  TestModel.fromJson(jsonObj)
    objFromJson.intProp should be(obj.intProp)
    objFromJson.boolProp should be(obj.boolProp)
    objFromJson.stringProp should be(obj.stringProp)
  }
}
