package integration.util.dao.mongo

import altitude.Altitude
import altitude.dao.mongo.BaseMongoDao
import altitude.transactions.TransactionId

class UtilitiesDao(val app: Altitude) extends BaseMongoDao("") with integration.util.dao.UtilitiesDao {
  override def dropDatabase(): Unit = {
    DB.dropDatabase()
    close()
    BaseMongoDao.client(app)
    val dbName = app.config.getString("db.mongo.db")
    val db = BaseMongoDao.client(app)(dbName)
    BaseMongoDao.gridFS(app, db, "preview")
  }

  override def close() = {
    BaseMongoDao.removeGridFS(app, "preview")
    BaseMongoDao.removeClient(app)
  }

  override def rollback() = Unit
  override def cleanupTest() = close()
  override def cleanupTests() = Unit
  override def createTransaction(tx: TransactionId) = Unit
}
