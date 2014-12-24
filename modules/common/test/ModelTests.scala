import java.io.File

import models.{AssetMediaType, FileImportAsset, BaseModel}
import org.scalatest._
import org.scalatest.Matchers._

class ModelTests extends FunSuite {
  test("model fields") {
    class TestModel extends BaseModel(id = "1")

    val model = new TestModel
    model.id shouldEqual "1"
    model.isClean shouldEqual false
  }

  test("import asset model") {
    val model = new FileImportAsset(new File("/"))
    model.id shouldBe null
    model.isClean shouldEqual false

    val d: Map[String, Any] = model.toMap
    d should contain key "id"
  }
}