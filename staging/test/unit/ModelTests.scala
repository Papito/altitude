package unit

import java.io.File

import altitude.models.{BaseModel, FileImportAsset}
import altitude.{Const => C}
import play.api.libs.json._

class ModelTests extends FunSuite {
  case class TestModel(id: Option[String] = None) extends BaseModel {
    def toJson: JsObject = coreJsonAttrs
  }

  test("create a model") {
    val model = TestModel()
  }

  test("import asset model") {
    val model = new FileImportAsset(new File("/"))
  }
}