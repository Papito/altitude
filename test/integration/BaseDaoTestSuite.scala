package integration

import altitude.dao.JdbcTransaction
import altitude.dao.mongo.BaseMongoDao
import altitude.dao.postgres.BasePostgresDao
import altitude.services.BaseService
import org.scalatest.{BeforeAndAfterAll, Suites}
import play.api.test.FakeApplication

/*
  Define base dao tests for each type of DB
 */

// MONGO
private class MongoBaseDaoTests(val config: Map[String, _])
  extends BaseDaoTests {

  class TestMongoDao extends BaseMongoDao("test")

  class TestMongoService extends BaseService[TestModel] {
    override protected val DAO = new TestMongoDao
  }

  override def service = new TestMongoService
}
class MongoBaseDaoTestSuite extends Suites(
  new MongoBaseDaoTests(Map("datasource" -> "mongo"))
)

// POSTGRES
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
      stmt.executeUpdate("DROP SCHEMA IF EXISTS \"altitude-test\" CASCADE; CREATE SCHEMA \"altitude-test\";")
      // we need this for the set of DAO tests
      stmt.executeUpdate("""
          |DROP TABLE IF EXISTS test;
          |CREATE TABLE test (
          | id varchar(24) NOT NULL,
          | created_at TIMESTAMP,
          | updated_at TIMESTAMP DEFAULT NULL)
        """.stripMargin)
    } finally {
      tx.close()
      play.api.Play.stop()
    }
  }
}
class PostgresBaseDaoTestSuite extends Suites(
  new PostgresBaseDaoTests(Map("datasource" -> "postgres"))
)