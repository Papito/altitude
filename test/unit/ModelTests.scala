package unit

import java.io.File

import altitude.models.{BaseModel, FileImportAsset}
import org.scalatest.Matchers._
import org.scalatest._
import play.api.libs.json.{JsObject, Json, JsValue}
import altitude.{Const => C}


class ModelTests extends FunSuite {
  test("model fields") {
    case class TestModel(override val id: Option[String] = None) extends BaseModel(id = None) {
      def toJson: JsObject = coreAttrs
    }

    val model = TestModel(Some("1"))
  }

  test("import asset model") {
    val model = new FileImportAsset(new File("/"))
  }
}