package integration

import altitude.models.BaseModel
import altitude.services.BaseService
import org.scalatest.Matchers._
import play.api.libs.json._

import scala.language.implicitConversions
import altitude.{Const => C}


trait BaseDaoTests extends IntegrationTestCore {
  def service: BaseService[TestModel]
  val model = new TestModel

  case class TestModel(id: Option[String] = None) extends BaseModel {
    def toJson: JsObject = coreJsonAttrs
  }

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
