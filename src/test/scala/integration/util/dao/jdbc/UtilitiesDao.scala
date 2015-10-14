package integration.util.dao.jdbc

import altitude.Altitude
import altitude.dao.jdbc.{VoidJdbcDao, BaseJdbcDao}
import altitude.models.BaseModel
import altitude.transactions.{JdbcTransactionManager, TransactionId}
import play.api.libs.json.{JsObject, Json}
import net.codingwell.scalaguice.InjectorExtensions._

class UtilitiesDao(app: Altitude) extends VoidJdbcDao(app) with integration.util.dao.UtilitiesDao {

  override def dropDatabase() = {}

  override def rollback() = {
    app.JDBC_TRANSACTIONS.foreach(tx => {
      tx._2.down()
      tx._2.rollback()
    })
  }

  override def close() = {
    app.JDBC_TRANSACTIONS.foreach(tx => {
      tx._2.down()
      tx._2.close()
    })
  }

  override def cleanupTest() = {
    rollback()
    close()
    app.JDBC_TRANSACTIONS.clear()
  }

  override def createTransaction(txId: TransactionId): Unit = {
    val tx = txManager.transaction(txId)
    tx.setReadOnly(false)
    tx.setAutoCommit(false)
    // up one level so it does not get committed or closed
    tx.up()
  }
}
