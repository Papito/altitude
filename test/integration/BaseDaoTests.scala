package integration

import altitude.models.BaseModel
import altitude.services.BaseService
import org.scalatest.Matchers._
import play.api.libs.json.{Json, JsValue}

import scala.language.implicitConversions
import altitude.{Const => C}

case class TestModel(id: String = BaseModel.genId) extends BaseModel(id) {
  override def toJson: JsValue = coreAttrs
}

trait BaseDaoTests extends IntegrationTestCore {
  def service: BaseService[TestModel]
  val model = new TestModel

  test("add record") {
    val future = service.add(model)
    val js: JsValue = future.futureValue
    val id = (js \ C.Base.ID).asOpt[String].getOrElse("")
    id should equal(model.id)

    // retrieve the object
    val future2 = service.getById(model.id)
    val js2: JsValue = future2.futureValue
    val id2 = (js2 \ C.Base.ID).asOpt[String].getOrElse("")
    id2 should equal(model.id)
  }
}
