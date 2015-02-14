package integration

import altitude.dao.mongo.BaseMongoDao
import altitude.services.BaseService
import org.scalatest.DoNotDiscover

@DoNotDiscover class BaseMongoDaoTests(val config: Map[String, _])
  extends IntegrationTestCore with BaseDaoTests {

  class TestMongoDao extends BaseMongoDao("test")

  class TestMongoService extends BaseService[TestModel] {
    override protected val DAO = new TestMongoDao
  }

  override def service = new TestMongoService
}
