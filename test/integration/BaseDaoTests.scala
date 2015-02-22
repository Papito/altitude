package integration

import altitude.models.BaseModel
import altitude.services.BaseService
import org.scalatest.FunSuite
import play.api.libs.json.{JsValue, JsObject, Json}

import scala.language.implicitConversions

object TestModel {
  implicit def toJson(obj: TestModel): JsValue = Json.obj(
    "id" -> obj.id
  )
}

class TestModel extends BaseModel

trait BaseDaoTests extends FunSuite {
  def service: BaseService[TestModel]
  val model = new TestModel

  test("add record") {
    service.add(model)
  }
}
