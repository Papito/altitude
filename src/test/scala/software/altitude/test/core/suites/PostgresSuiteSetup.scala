package software.altitude.test.core.suites

import java.sql.DriverManager
import java.util.Properties

import org.scalatest.{Suite, BeforeAndAfterAll}
import org.slf4j.LoggerFactory
import software.altitude.core.{Configuration, Environment}
import software.altitude.test.core.integration.IntegrationTestCore

trait PostgresSuiteSetup extends Suite with BeforeAndAfterAll {
  Environment.ENV = Environment.TEST
  val log =  LoggerFactory.getLogger(getClass)

  override def beforeAll(): Unit = {
    IntegrationTestCore.createTestDir(PostgresSuite.app)

    log.info("TEST. Resetting DB schema once")
    DriverManager.registerDriver(new org.postgresql.Driver)
    val appConfig = new Configuration()
    val props = new Properties
    val user = appConfig.getString("db.postgres.user")
    props.setProperty("user", user)
    val password = appConfig.getString("db.postgres.password")
    props.setProperty("password", password)
    val url = appConfig.getString("db.postgres.url")

    log.info("Clearing postgres database")
    val sql = "DROP SCHEMA IF EXISTS \"altitude-test\" CASCADE; CREATE SCHEMA \"altitude-test\";"

    val conn = DriverManager.getConnection(url, props)
    val stmt = conn.createStatement()
    try {
      stmt.executeUpdate(sql)
    }
    finally {
      if (stmt != null) stmt.close()
      if (conn != null) conn.close()
    }

    PostgresSuite.app.service.migration.migrate()
    log.info("END SETUP")
  }
}
