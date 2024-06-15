package software.altitude.test.core.integration.util.dao

import net.codingwell.scalaguice.InjectorExtensions._
import software.altitude.core.AltitudeCoreApp
import software.altitude.core.transactions.JdbcTransactionManager
import software.altitude.core.transactions.TransactionId

class UtilitiesDao(app: AltitudeCoreApp)  {

  protected final def txManager: JdbcTransactionManager = app.injector.instance[JdbcTransactionManager]

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
