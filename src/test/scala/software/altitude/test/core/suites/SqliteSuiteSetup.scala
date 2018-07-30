package software.altitude.test.core.suites

import java.sql.DriverManager

import org.scalatest.{BeforeAndAfterAll, Suite}
import org.slf4j.LoggerFactory
import software.altitude.core.{Configuration, Environment}
import software.altitude.test.core.integration.IntegrationTestCore

trait SqliteSuiteSetup extends Suite with BeforeAndAfterAll {
  Environment.ENV = Environment.TEST
  protected final val log = LoggerFactory.getLogger(getClass)

  override def beforeAll(): Unit = {
    IntegrationTestCore.createTestDir(SqliteSuite.app)

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

    SqliteSuite.app.service.migrationService.migrate()
    log.info("END SETUP")
  }
}
