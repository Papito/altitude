package software.altitude.test.core.integration.util.dao

import software.altitude.core.AltitudeAppContext
import software.altitude.core.transactions.TransactionId
import software.altitude.core.transactions.TransactionManager

class UtilitiesDao(app: AltitudeAppContext)  {

  protected final def txManager: TransactionManager = app.txManager

  def rollback(): Unit = {

    txManager.txRegistry.foreach(tx => {
      tx._2.down()
      tx._2.rollback()
    })
  }

  def close(): Unit = {
    txManager.txRegistry.foreach(tx => {
      tx._2.down()
      txManager.closeTransaction(tx._2)
    })
  }

  def cleanupTest(): Unit = {
    rollback()
    close()
    txManager.txRegistry.clear()
  }

  def createTransaction(txId: TransactionId): Unit = {
    val tx = txManager.transaction()(txId)
    // up one level so it does not get committed or closed
    tx.up()
  }
}
