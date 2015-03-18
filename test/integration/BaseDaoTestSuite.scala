package integration

import altitude.dao.mongo.BaseMongoDao
import altitude.dao.postgres.BasePostgresDao
import altitude.services.BaseService
import org.scalatest.Suites

/*
  Define base dao tests for each type of DB.
  The suite at the bottom runs all
 */
private class MongoBaseDaoTests(val config: Map[String, _])
  extends BaseDaoTests {

  class TestMongoDao extends BaseMongoDao("test")

  class TestMongoService extends BaseService[TestModel] {
    override protected val DAO = new TestMongoDao
  }

  override def service = new TestMongoService
}

private class PostgresBaseDaoTests(val config: Map[String, _])
  extends BaseDaoTests {

  class TestPostgresDao extends BasePostgresDao("test")

  class TestPostgresService extends BaseService[TestModel] {
    override protected val DAO = new TestPostgresDao
  }

  override def service = new TestPostgresService
}

class BaseDaoTestSuite extends Suites(
  new MongoBaseDaoTests(Map("datasource" -> "mongo")),
  new PostgresBaseDaoTests(Map("datasource" -> "postgres"))
)