package integration

import altitude.{Altitude, Environment}
import altitude.transactions.{TransactionId, Transaction, JdbcTransactionManager, JdbcTransaction}
import org.scalatest.BeforeAndAfterAll
import org.slf4j.LoggerFactory

class PostgresSuite extends AllTests(config = Map("datasource" -> "postgres")) with BeforeAndAfterAll {
  Environment.ENV = Environment.TEST
  val log =  LoggerFactory.getLogger(getClass)

  override def beforeAll(): Unit = {
    //Reset the database schema once
    val altitude: Altitude = new Altitude
    implicit val txId: TransactionId = new TransactionId

    val txManager = new JdbcTransactionManager(altitude)

    txManager.withTransaction {
      log.info("SETUP")
      val stmt = txManager.transaction.conn.createStatement()
      stmt.executeUpdate("DROP SCHEMA IF EXISTS \"altitude-test\" CASCADE; CREATE SCHEMA \"altitude-test\";")
      stmt.executeUpdate("""
                           |DROP TABLE IF EXISTS test;
                           |CREATE TABLE test (
                           | id varchar(24) NOT NULL,
                           | created_at TIMESTAMP,
                           | updated_at TIMESTAMP)
                         """.stripMargin)
    }
    /*
      We have to commit this, however, later we make sure everything is rolled back.
      The committed count must be kept at zero
    */
    log.info("END SETUP")
    Transaction.COMMITTED = 0
 }
}