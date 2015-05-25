package integration

import altitude.models.BaseModel
import altitude.service.BaseService
import altitude.{Const => C}
import play.api.libs.json._
import org.scalatest.Matchers._
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
    val js = service.add(model)

    js \ C.Base.ID should not be Nil
    //js \ "_id" should be Nil FIXME
    val id = (js \ C.Base.ID).as[String]
    js \ C.Base.CREATED_AT should not be Nil
    js \ C.Base.CREATED_AT should be (an[JsString])
    js \ C.Base.UPDATED_AT should be(JsNull)

    // retrieve the object
    val js2Opt: Option[JsObject] = service.getById(id)
    js2Opt should not be None

    val js2 = js2Opt.get
    js2 \ C.Base.CREATED_AT should be (an[JsString])
    js2 \ C.Base.UPDATED_AT should be(JsNull)

    val model2 = js2: TestModel
    model2.id.get should be(id)
    model2.createdAt should not be None
    model2.updatedAt should be(None)
  }
}
