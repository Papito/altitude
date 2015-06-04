package integration

import java.io.File
import scala.io.Source._

import altitude.{Altitude, Environment}
import altitude.transactions.{TransactionId, Transaction, JdbcTransactionManager, JdbcTransaction}
import org.apache.commons.io.FileUtils
import org.scalatest.{DoNotDiscover, BeforeAndAfterAll}
import org.slf4j.LoggerFactory

class PostgresSuite extends AllTests(config = Map("datasource" -> "postgres"))
  with BeforeAndAfterAll {
  Environment.ENV = Environment.TEST
  val log =  LoggerFactory.getLogger(getClass)

  override def beforeAll(): Unit = {
    //Reset the database schema once

    val altitude: Altitude = new Altitude
    implicit val txId: TransactionId = new TransactionId
    val txManager = new JdbcTransactionManager(altitude)

    val evolutionPath = new java.io.File( "evolutions/postgres/1.sql" ).getCanonicalPath
    val sql = scala.io.Source.fromFile(evolutionPath).mkString
    log.info(s"Running $sql")

    txManager.withTransaction {
      log.info("SETUP")
      val stmt = txManager.transaction.conn.createStatement()
      stmt.executeUpdate(sql)
    }
    /*
      We have to commit this, however, later we make sure everything is rolled back.
      The committed count must be kept at zero
    */
    log.info("END SETUP")
    Transaction.COMMITTED = 0
 }
}