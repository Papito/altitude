import java.io.File

import models.{ImportAsset, BaseModel}
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
    val model = new ImportAsset(new File("/"))
    model.id shouldBe null
    model.isClean shouldEqual false
  }
}