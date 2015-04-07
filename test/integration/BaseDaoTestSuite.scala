package integration

import altitude.dao.JdbcTransaction
import altitude.dao.mongo.BaseMongoDao
import altitude.dao.postgres.BasePostgresDao
import altitude.services.BaseService
import org.scalatest.{BeforeAndAfterAll, Suites}
import play.api.test.FakeApplication

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
  extends BaseDaoTests with BeforeAndAfterAll {

  class TestPostgresDao extends BasePostgresDao("test")

  class TestPostgresService extends BaseService[TestModel] {
    override protected val DAO = new TestPostgresDao
  }

  override def service = new TestPostgresService

  override def beforeAll(): Unit = {
    /*
    Reset the database schema once and run the migrations
     */
    val app = FakeApplication(additionalConfiguration = Map("datasource" -> "postgres"))
    play.api.Play.start(app)

    val tx = new JdbcTransaction
    try {
      val stmt = tx.conn.createStatement()
      stmt.executeUpdate("DROP SCHEMA PUBLIC CASCADE; CREATE SCHEMA PUBLIC;")
      stmt.executeUpdate("CREATE TABLE test (id varchar(24) NOT NULL);")
    } finally {
      tx.close()
      play.api.Play.stop()
    }
  }
}

class BaseDaoTestSuite extends Suites(
  new MongoBaseDaoTests(Map("datasource" -> "mongo")),
  new PostgresBaseDaoTests(Map("datasource" -> "postgres"))
)