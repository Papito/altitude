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
    model.createdAt should be(None)
    model.updatedAt should be(None)
    val future = service.add(model)
    val js: JsObject = future.futureValue

    js \ C.Base.ID should not be Nil
    val id = (js \ C.Base.ID).asOpt[String]
    id should not be None

    // retrieve the object
    val persisted = service.getById(id.get)
    val persistedJs: JsObject = persisted.futureValue
    persistedJs \ C.Base.ID should not be Nil
  }
}
