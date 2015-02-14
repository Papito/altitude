package integration

import altitude.models.BaseModel
import altitude.services.BaseService
import org.scalatest.{FunSuite, DoNotDiscover}
import play.api.libs.json.{Json, JsObject}

class TestModel extends BaseModel {
  override def toJson: JsObject = Json.obj(
    "id" -> id
  )
}

trait BaseDaoTests extends FunSuite {
  def service: BaseService[TestModel]
  val model = new TestModel

  test("add record") {
    service.add(model)
  }
}
