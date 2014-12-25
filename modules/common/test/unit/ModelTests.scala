package unit

import models.BaseModel
import org.scalatest.Matchers._
import org.scalatest._

class ModelTests extends FunSuite {
  test("model fields") {
    class TestModel extends BaseModel(id = "1")

    val model = new TestModel
    model.id shouldEqual "1"
    model.isClean shouldEqual false
  }
}