package unit

import java.io.File

import altitude.models.{StoreLocation, Asset, BaseModel, FileImportAsset}
import org.scalatest.Matchers._
import org.scalatest._
import altitude.{Const => C}
import play.api.libs.json._

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

class ModelTests extends FunSuite {
  test("create a model") {


    val model = TestModel(id = Some("1"))
  }

  test("import asset model") {
    val model = new FileImportAsset(new File("/"))
  }
}