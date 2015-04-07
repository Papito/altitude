package unit

import java.io.File

import altitude.models.{BaseModel, FileImportAsset}
import org.scalatest.Matchers._
import org.scalatest._

class ModelTests extends FunSuite {
  test("model fields") {
    class TestModel extends BaseModel

    val model = new TestModel
    model.id should not be None
  }

  test("import asset model") {
    val model = new FileImportAsset(new File("/"))
  }
}