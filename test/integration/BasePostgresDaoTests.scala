package integration

import java.io.File

import altitude.dao.LibraryDao
import altitude.dao.postgres.BasePostgresDao
import altitude.models.{BaseModel, Asset, FileImportAsset}
import altitude.services.BaseService
import org.scalatest.DoNotDiscover
import play.api.libs.json.{Json, JsObject, JsValue}
import reactivemongo.bson.BSONObjectID

import scala.concurrent.Future

@DoNotDiscover class BasePostgresDaoTests(val config: Map[String, _]) extends IntegrationTestCore {
  class TestModel extends BaseModel[String] {
    override protected def genId: String = BSONObjectID.generate.stringify

    override def toJson: JsObject = Json.obj(
      "id" -> id
    )
  }

  class TestPostgresDao extends BasePostgresDao("test")

  class TestService extends BaseService[TestModel, String] {
    override protected val DAO = new TestPostgresDao
  }

  test("add record") {
    val model = new TestModel
    new TestService().add(model)
  }
}
