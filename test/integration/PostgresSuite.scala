package integration

import altitude.dao.JdbcTransaction
import org.scalatest.BeforeAndAfterAll
import play.api.test.FakeApplication

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
    } finally {
      tx.close()
      play.api.Play.stop()
    }
  }
}