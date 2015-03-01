package integration

import altitude.models.{Asset, BaseModel}
import altitude.services.BaseService
import org.scalatest.Matchers._
import play.api.libs.json.{JsValue, JsObject, Json}


import scala.concurrent.Future
import scala.language.implicitConversions

object TestModel {
  implicit def toJson(obj: TestModel): JsValue = Json.obj(
    "id" -> obj.id
  )
}

class TestModel extends BaseModel

trait BaseDaoTests extends IntegrationTestCore {
  def service: BaseService[TestModel]
  val model = new TestModel

  test("add record") {
    val f: Future[JsValue] = service.add(model)
    whenReady(f) {json =>
      (json \ "id").as[String] should be(model.id)
    }
  }
}
