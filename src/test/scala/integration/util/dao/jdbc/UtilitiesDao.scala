package integration.util.dao.jdbc

import altitude.{Const, Altitude}
import altitude.dao.jdbc.BaseJdbcDao
import altitude.dao.postgres.Postgres
import altitude.models.BaseModel
import altitude.transactions.TransactionId
import org.joda.time.DateTime

class UtilitiesDao(val app: Altitude) extends BaseJdbcDao("") with integration.util.dao.UtilitiesDao {
  protected def CORE_SQL_VALS_FOR_INSERT = ""
  protected def DEFAULT_SQL_COLS_FOR_SELECT = ""
  protected def addCoreAttrs(model: BaseModel, rec: Map[String, AnyRef]): Unit = Unit

  override def dropDatabase() = Unit

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

  override def cleanupTests() = Unit

  override def createTransaction(txId: TransactionId): Unit = {
    val tx = jdbcTxManager.transaction(txId)
    tx.setReadOnly(flag = false)
    tx.setAutoCommit(flag = false)
    // up one level so it does not get committed or closed
    tx.up()
  }
}
