package integration.util.dao.mongo

import altitude.Altitude
import altitude.dao.mongo.BaseMongoDao
import altitude.transactions.TransactionId

class UtilitiesDao(val app: Altitude) extends BaseMongoDao("") with integration.util.dao.UtilitiesDao {
  override def dropDatabase(): Unit = {
    DB.dropDatabase()
  }

  override def close() = {
    val client = BaseMongoDao.CLIENTS.get(app.id)
    if (client.isDefined) {
      client.get.close()
    }
  }
  override def rollback() = Unit
  override def cleanup() = Unit
  override def createTransaction(tx: TransactionId) = Unit
}
