package integration.util.dao.mongo

import altitude.Altitude
import altitude.dao.mongo.BaseMongoDao
import altitude.transactions.TransactionId

class UtilitiesDao(val app: Altitude) extends BaseMongoDao("") with integration.util.dao.UtilitiesDao {
  override def dropDatabase(): Unit = {
    db.dropDatabase()
  }

  override def close() = Unit
  override def rollback() = Unit
  override def cleanup() = Unit
  override def createTransaction(tx: TransactionId) = Unit
}
