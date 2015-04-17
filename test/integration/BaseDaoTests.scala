package integration

import altitude.models.BaseModel
import altitude.services.BaseService
import org.scalatest.Matchers._
import play.api.libs.json._

import scala.language.implicitConversions
import altitude.{Const => C}

object TestModel {
  implicit val writes = new Writes[TestModel] {
    def writes(o: TestModel) = Json.obj(
      C.Asset.ID -> o.id
    )
  }

  implicit val reads = new Reads[TestModel] {
    def reads(json: JsValue): JsResult[TestModel] = JsSuccess {
      TestModel(
        id = (json \ C.Base.ID).asOpt[String]
      )}}
}

case class TestModel(id: Option[String] = None) extends BaseModel {
  def toJson = Json.toJson(this).as[JsObject]
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
