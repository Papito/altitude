package integration

import altitude.Altitude
import altitude.dao.postgres.BasePostgresDao
import altitude.service.BaseService
import altitude.transactions.{Transaction, JdbcTransactionManager, JdbcTransaction}
import org.scalatest.{Suites, BeforeAndAfterAll}

/*
  Define base dao tests for each type of DB
 */

// MONGO
/*
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
*/

// POSTGRES
private class PostgresBaseDaoTests(val config: Map[String, _])
  extends BaseDaoTests with BeforeAndAfterAll {

  class TestPostgresDao(val app: Altitude = app) extends BasePostgresDao("test1")

  class TestPostgresService extends BaseService[TestModel](app) {
    override protected val DAO = new TestPostgresDao
  }

  override def service = new TestPostgresService

  override def beforeAll(): Unit = {
    /*
    Reset the database schema once
     */
    val txManager = new JdbcTransactionManager(app)

    txManager.withTransaction {
      val stmt = txManager.transaction.conn.createStatement()
      // we need this for the set of DAO tests
      stmt.executeUpdate("""
                           |DROP SCHEMA IF EXISTS "altitude-test" CASCADE; CREATE SCHEMA "altitude-test";
                           |DROP TABLE IF EXISTS test2;
                           |DROP TABLE IF EXISTS test1;
                           |DROP TABLE IF EXISTS _test_core;
                           |
                           |CREATE TABLE _test_core (
                           | id VARCHAR(24) NOT NULL,
                           | created_at timestamp WITHOUT TIME ZONE,
                           | updated_at timestamp WITHOUT TIME ZONE DEFAULT NULL);
                           |
                           |CREATE TABLE test1 (
                           | field1_1 INTEGER,
                           | field1_2 VARCHAR(255)) INHERITS (_test_core);
                           |
                           |CREATE TABLE test2 (
                           | field2_1 INTEGER,
                           | field2_2 VARCHAR(255)) INHERITS (_test_core);
                         """.stripMargin)
    }
    Transaction.COMMITTED = 0
  }
}
class PostgresBaseDaoTestSuite extends Suites(
  new PostgresBaseDaoTests(Map("datasource" -> "postgres"))
)