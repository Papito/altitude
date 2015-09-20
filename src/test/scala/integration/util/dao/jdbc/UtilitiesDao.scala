package integration.util.dao.jdbc

import altitude.Altitude
import altitude.dao.jdbc.BaseJdbcDao
import altitude.models.BaseModel
import altitude.transactions.TransactionId
import play.api.libs.json.{JsObject, Json}

class UtilitiesDao(val app: Altitude) extends BaseJdbcDao("") with integration.util.dao.UtilitiesDao {
  protected def CORE_SQL_VALS_FOR_INSERT = ""
  protected def DEFAULT_SQL_COLS_FOR_SELECT = ""
  protected def JSON_PLACEHOLDER = ""
  protected def addCoreAttrs(model: BaseModel, rec: Map[String, AnyRef]): Unit = Unit
  protected def makeModel(rec: Map[String, AnyRef]): JsObject = Json.obj()

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
