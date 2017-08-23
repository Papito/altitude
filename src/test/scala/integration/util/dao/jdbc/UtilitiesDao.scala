package integration.util.dao.jdbc

import net.codingwell.scalaguice.InjectorExtensions._
import software.altitude.core.{AltitudeCoreApp, Altitude}
import software.altitude.core.transactions.{JdbcTransactionManager, TransactionId}

class UtilitiesDao(app: AltitudeCoreApp) extends integration.util.dao.UtilitiesDao {

  protected final def txManager = app.injector.instance[JdbcTransactionManager]

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
