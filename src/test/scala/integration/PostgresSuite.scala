package integration

import java.sql.DriverManager
import java.util.Properties

import altitude.transactions.{JdbcTransactionManager, Transaction, TransactionId}
import altitude.{Configuration, Altitude, Environment}
import org.scalatest.BeforeAndAfterAll
import org.slf4j.LoggerFactory

class PostgresSuite extends AllTests(config = Map("datasource" -> "postgres"))
  with BeforeAndAfterAll {
  Environment.ENV = Environment.TEST
  val log =  LoggerFactory.getLogger(getClass)

  override def beforeAll(): Unit = {
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
    val sql =
      """
        |DROP SCHEMA IF EXISTS "altitude-test" CASCADE; CREATE SCHEMA "altitude-test";
      """.stripMargin

    val conn = DriverManager.getConnection(url, props)
    val stmt = conn.createStatement()
    try {
      stmt.executeUpdate(sql)
    }
    finally {
      if (stmt != null) stmt.close()
      if (conn != null) conn.close()
    }

    IntegrationTestCore.postgresApp.service.migration.migrate()
    log.info("END SETUP")
 }
}