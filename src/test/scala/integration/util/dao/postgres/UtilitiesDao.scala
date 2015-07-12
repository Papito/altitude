package integration.util.dao.postgres

import altitude.Altitude
import altitude.dao.postgres.BasePostgresDao
import altitude.transactions.{JdbcTransactionManager, TransactionId}

class UtilitiesDao(val app: Altitude) extends BasePostgresDao("") with integration.util.dao.UtilitiesDao {
  override def dropDatabase() = Unit

  override def rollback() = {
    JdbcTransactionManager.TRANSACTIONS.foreach(tx => {
      tx._2.down()
      tx._2.rollback()
    })
  }

  override def close() = {
    JdbcTransactionManager.TRANSACTIONS.foreach(tx => {
      tx._2.down()
      tx._2.close()
    })
  }

  override def cleanup() = {
    rollback()
    close()
    JdbcTransactionManager.TRANSACTIONS.clear()
  }

  override def createTransaction(txId: TransactionId): Unit = {
    val tx = jdbcTxManager.transaction(txId)
    tx.setReadOnly(flag = false)
    tx.setAutoCommit(flag = false)
    // up one level so it does not get committed or closed
    tx.up()
  }
}
