package integration

import altitude.models.BaseModel
import altitude.services.BaseService
import altitude.{Const => C}
import org.scalatest.Matchers._
import play.api.libs.json._

import scala.language.implicitConversions


trait BaseDaoTests extends IntegrationTestCore {

  object TestModel {
    implicit def fromJson(json: JsValue): TestModel = TestModel(
      id = (json \ C.Asset.ID).asOpt[String]
    ).withCoreAttr(json)
  }

  case class TestModel(id: Option[String] = None) extends BaseModel {
    def toJson: JsObject = coreJsonAttrs
  }

  def service: BaseService[TestModel]
  val model = new TestModel

  test("add record") {
    model.id should be(None)
    val future = service.add(model)
    val js: JsObject = future.futureValue

    js \ C.Base.ID should not be Nil
    //js \ "_id" should be Nil FIXME
    val id = (js \ C.Base.ID).as[String]
    js \ C.Base.CREATED_AT should not be Nil
    js \ C.Base.CREATED_AT should be (an[JsString])
    js \ C.Base.UPDATED_AT should be(JsNull)

    // retrieve the object
    val future2 = service.getById(id)
    val js2: JsObject = future2.futureValue
    js \ C.Base.CREATED_AT should be (an[JsString])
    js \ C.Base.UPDATED_AT should be(JsNull)

    val model2 = js2: TestModel
    model2.id.get should be(id)
    model2.createdAt should not be None
    model2.updatedAt should be(None)
  }
}
