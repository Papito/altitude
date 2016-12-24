package integration.util.dao.mongo

import altitude.dao.mongo.BaseMongoDao
import altitude.transactions.TransactionId
import altitude.{Altitude, Context}
import integration.MongoSuite

class UtilitiesDao(val app: Altitude) extends BaseMongoDao("") with integration.util.dao.UtilitiesDao {
  override def migrateDatabase(): Unit = {
    BaseMongoDao.DB.get.dropDatabase()
    MongoSuite.app.service.migration.migrate()
  }

  override def close() = Unit
  override def rollback() = Unit
  override def cleanupTest() = Unit
  override def createTransaction(txId: TransactionId) = Unit
}
