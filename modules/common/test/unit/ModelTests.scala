package unit

import models.common.BaseModel
import org.scalatest.Matchers._
import org.scalatest._
import play.api.libs.json.{JsObject, Json}
import reactivemongo.bson.BSONObjectID

class ModelTests extends FunSuite {
  test("model fields") {
    class TestModel extends BaseModel[String] {
      override def toJson: JsObject = Json.obj()
      override protected def genId: String = BSONObjectID.generate.toString()
    }

    val model = new TestModel
    model.id should not be None
    model.isClean shouldEqual false
  }
}