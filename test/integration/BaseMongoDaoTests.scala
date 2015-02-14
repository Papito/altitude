package integration

import altitude.dao.mongo.BaseMongoDao
import altitude.models.BaseModel
import altitude.services.BaseService
import org.scalatest.DoNotDiscover
import play.api.libs.json.{Json, JsObject}

@DoNotDiscover class BaseMongoDaoTests(val config: Map[String, _]) extends IntegrationTestCore {
  class TestModel extends BaseModel {
    override def toJson: JsObject = Json.obj(
      "id" -> id
    )
  }

  class TestPostgresDao extends BaseMongoDao("test")

  class TestService extends BaseService[TestModel] {
    override protected val DAO = new TestPostgresDao
  }

  test("add record") {
    val model = new TestModel
    new TestService().add(model)
  }
}
