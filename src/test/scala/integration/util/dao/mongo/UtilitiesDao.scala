package integration.util.dao.mongo

import altitude.Altitude
import altitude.dao.mongo.BaseMongoDao
import altitude.transactions.TransactionId

class UtilitiesDao(val app: Altitude) extends BaseMongoDao("") with integration.util.dao.UtilitiesDao {
  override def dropDatabase(): Unit = {
    DB.dropDatabase()
    close()
    BaseMongoDao.client(app)
    BaseMongoDao.gridFS(app, DB, "preview")
  }

  override def close() = {
    BaseMongoDao.removeGridFS(app, "preview")
    BaseMongoDao.removeClient(app)
  }
  override def rollback() = Unit
  override def cleanup() = close()
  override def createTransaction(tx: TransactionId) = Unit
}
