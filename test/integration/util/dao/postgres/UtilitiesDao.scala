package integration.util.dao.postgres

import altitude.dao.{Transaction, TransactionId}
import altitude.dao.postgres.BasePostgresDao
import altitude.services.JdbcTransactionManager
import altitude.util.log

class UtilitiesDao extends BasePostgresDao("") with integration.util.dao.UtilitiesDao {
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

    JdbcTransactionManager.TRANSACTIONS.clear()
  }

  override def createTransaction(txId: TransactionId): Unit = {
    val tx = JdbcTransactionManager.transaction(txId)
    tx.setReadOnly(flag = false)
    tx.setAutoCommit(flag = false)
    // up one level so it does not get committed or closed
    tx.up()
  }
}
