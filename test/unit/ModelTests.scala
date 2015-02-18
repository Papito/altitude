package unit

import java.io.File

import altitude.models.{BaseModel, FileImportAsset}
import org.scalatest.Matchers._
import org.scalatest._
import play.api.libs.json.{JsObject, Json}

class ModelTests extends FunSuite {
  test("model fields") {
    class TestModel extends BaseModel {
      override def toJson: JsObject = Json.obj()
    }

    val model = new TestModel
    model.id should not be None
    model.isClean shouldEqual false
  }

  test("import asset model") {
    val model = new FileImportAsset(new File("/"))
    model.isClean shouldEqual false

    val j: JsObject = model.toJson
  }
}