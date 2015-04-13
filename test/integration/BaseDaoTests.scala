package integration

import altitude.models.BaseModel
import altitude.services.BaseService
import org.scalatest.Matchers._
import play.api.libs.json.{JsObject, Json, JsValue}

import scala.language.implicitConversions
import altitude.{Const => C}

case class TestModel(override val id: Option[String] = None) extends BaseModel(id) {
  override def toJson: JsObject = coreAttrs
}

trait BaseDaoTests extends IntegrationTestCore {
  def service: BaseService[TestModel]
  val model = new TestModel

  test("add record") {
    val future = service.add(model)
    val js: JsObject = future.futureValue
    val id = (js \ C.Base.ID).asOpt[String].getOrElse("")
    id should equal(model.id)

    // retrieve the object
    val future2 = service.getById(model.id.get)
    val js2: JsObject = future2.futureValue
    val id2 = (js2 \ C.Base.ID).asOpt[String].getOrElse("")
    id2 should equal(model.id)
  }
}
