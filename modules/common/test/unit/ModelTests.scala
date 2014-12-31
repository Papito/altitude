package unit

import models.BaseModel
import org.scalatest.Matchers._
import org.scalatest._

class ModelTests extends FunSuite {
  test("model fields") {
    class TestModel extends BaseModel

    val model = new TestModel
    model.id shouldEqual None
    model.isClean shouldEqual false
  }
}