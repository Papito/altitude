package integration

import altitude.models.BaseModel
import altitude.services.BaseService
import org.scalatest.Matchers._
import play.api.libs.json.{JsObject, Json, JsValue}

import scala.language.implicitConversions
import altitude.{Const => C}

case class TestModel(id: Option[String] = None) extends BaseModel {
  override def toJson: JsObject = coreAttrs
}

trait BaseDaoTests extends IntegrationTestCore {
  def service: BaseService[TestModel]
  val model = new TestModel

  test("add record") {
    model.id should be(None)
    val future = service.add(model)
    val js: JsObject = future.futureValue

    js \ C.Base.ID should not be Nil
    val id = (js \ C.Base.ID).as[String]

    // retrieve the object
    val future2 = service.getById(id)
    val js2: JsObject = future2.futureValue
  }
}
