package integration.util.dao.jdbc

import altitude.Altitude
import altitude.dao.jdbc.VoidJdbcDao
import altitude.transactions.TransactionId

class UtilitiesDao(app: Altitude) extends VoidJdbcDao(app) with integration.util.dao.UtilitiesDao {

  override def migrateDatabase() = {}

  override def rollback() = {

    txManager.txRegistry.foreach(tx => {
      tx._2.down()
      tx._2.rollback()
    })
  }

  override def close() = {
    txManager.txRegistry.foreach(tx => {
      tx._2.down()
      txManager.closeTransaction(tx._2)
    })
  }

  override def cleanupTest() = {
    rollback()
    close()
    txManager.txRegistry.clear()
  }

  override def createTransaction(txId: TransactionId): Unit = {
    val tx = txManager.transaction()(txId)
    // up one level so it does not get committed or closed
    tx.up()
  }
}
