package integration.util.dao.jdbc

import altitude.dao.jdbc.VoidJdbcDao
import altitude.{Altitude, Context}

class UtilitiesDao(app: Altitude) extends VoidJdbcDao(app) with integration.util.dao.UtilitiesDao {

  override def migrateDatabase() = {}

  override def rollback() = {

    txManager.txContainer.foreach(tx => {
      tx._2.down()
      tx._2.rollback()
    })
  }

  override def close() = {
    txManager.txContainer.foreach(tx => {
      tx._2.down()
      txManager.closeTransaction(tx._2)
    })
  }

  override def cleanupTest() = {
    rollback()
    close()
    txManager.txContainer.clear()
  }

  override def createTransaction(ctx: Context): Unit = {
    val tx = txManager.transaction(ctx, readOnly = false)
    // up one level so it does not get committed or closed
    tx.up()
  }
}
