package software.altitude.test.core.suites

import java.sql.DriverManager

import org.scalatest.{BeforeAndAfterAll, Suite}
import org.slf4j.LoggerFactory
import software.altitude.core.{Configuration, Environment}

trait SqliteSuiteSetup extends Suite with BeforeAndAfterAll {
  Environment.ENV = Environment.TEST
  val log =  LoggerFactory.getLogger(getClass)

  override def beforeAll(): Unit = {
    log.info("TEST. Resetting DB schema once")
    val url: String = new Configuration().getString("db.sqlite.url")
    DriverManager.registerDriver(new org.sqlite.JDBC)

    log.info("Clearing sqlite database")
    val sql =
      """
        PRAGMA writable_schema = 1;
        delete from sqlite_master where type in ('table', 'index', 'trigger');
        PRAGMA writable_schema = 0;
        VACUUM;
        PRAGMA INTEGRITY_CHECK;
      """.stripMargin

    val conn = DriverManager.getConnection(url)
    val stmt = conn.createStatement()

    try {
      stmt.executeUpdate(sql)
    }
    finally {
      if (stmt != null) stmt.close()
      if (conn != null) conn.close()
    }

    SqliteSuite.app.service.migration.migrate()
    log.info("END SETUP")
  }
}
