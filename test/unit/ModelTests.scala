package unit

import java.io.File

import altitude.models.{BaseModel, FileImportAsset}
import org.scalatest.Matchers._
import org.scalatest._
import play.api.libs.json.{JsObject, Json, JsValue}
import altitude.{Const => C}


class ModelTests extends FunSuite {
  test("create a model") {
    case class TestModel(id: Option[String] = None) extends BaseModel {
      def toJson: JsObject = coreAttrs
    }

    val model = TestModel(id = Some("1"))
  }

  test("import asset model") {
    val model = new FileImportAsset(new File("/"))
  }
}