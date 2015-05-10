package integration

import altitude.dao.JdbcTransaction

class PostgresSuite extends AllTests(config = Map("datasource" -> "postgres")) with BeforeAndAfterAll {

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
      stmt.executeUpdate("""
                           |DROP TABLE IF EXISTS test;
                           |CREATE TABLE test (
                           | id varchar(24) NOT NULL,
                           | created_at TIMESTAMP,
                           | updated_at TIMESTAMP)
                         """.stripMargin)
    } finally {
      tx.close()
      play.api.Play.stop()
    }
  }
}