package integration

import java.sql.DriverManager

import altitude.transactions.{JdbcTransactionManager, TransactionId}
import altitude.{Configuration, Altitude, Environment}
import org.scalatest.{DoNotDiscover, BeforeAndAfterAll}
import org.slf4j.LoggerFactory

class SqliteSuite extends AllTests(config = Map("datasource" -> "sqlite")) with BeforeAndAfterAll {
  Environment.ENV = Environment.TEST
  val log =  LoggerFactory.getLogger(getClass)

  override def beforeAll(): Unit = {
    log.info("TEST. Resetting DB schema once")
    val url: String = new Configuration().getString("db.sqlite.url")
    log.info(url)
    DriverManager.registerDriver(new org.sqlite.JDBC)

    val sql =
      """
        |PRAGMA writable_schema = 1;
        |delete from sqlite_master where type in ('table', 'index', 'trigger');
        |PRAGMA writable_schema = 0;
        |VACUUM;
        |PRAGMA INTEGRITY_CHECK;
      """.stripMargin

    val conn = DriverManager.getConnection(url)

    val stmt = conn.createStatement()
    stmt.executeUpdate(sql)
    stmt.close()
    conn.close()

    val altitude: Altitude = new Altitude(config)
    /*
      We have to commit this, however, later we make sure everything is rolled back.
      The committed count must be kept at zero
    */
    log.info("END SETUP")
  }
}
