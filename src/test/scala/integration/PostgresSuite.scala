package integration

import altitude.transactions.{JdbcTransactionManager, Transaction, TransactionId}
import altitude.{Altitude, Environment}
import org.scalatest.BeforeAndAfterAll
import org.slf4j.LoggerFactory

class PostgresSuite extends AllTests(config = Map("datasource" -> "postgres"))
  with BeforeAndAfterAll {
  Environment.ENV = Environment.TEST
  val log =  LoggerFactory.getLogger(getClass)

  override def beforeAll(): Unit = {
    log.info("TEST. Resetting DB schema once")

    val evolutionPath = new java.io.File( "evolutions/postgres/1.sql" ).getCanonicalPath
    val sql = scala.io.Source.fromFile(evolutionPath).mkString
    //log.info(s"Running $sql")

    val altitude: Altitude = new Altitude(config)
    implicit val txId: TransactionId = new TransactionId
    val txManager = new JdbcTransactionManager(altitude)

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
 }
}