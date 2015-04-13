package unit

import java.io.File

import altitude.models.{BaseModel, FileImportAsset}
import org.scalatest.Matchers._
import org.scalatest._
import play.api.libs.json.{Json, JsValue}
import altitude.{Const => C}


class ModelTests extends FunSuite {
  test("model fields") {
    case class TestModel(id: String = BaseModel.genId) extends BaseModel(id = id) {
      def toJson: JsValue = Json.obj(C.Base.ID -> id)
    }

    val model = new TestModel
    model.id should not be None
  }

  test("import asset model") {
    val model = new FileImportAsset(new File("/"))
  }
}