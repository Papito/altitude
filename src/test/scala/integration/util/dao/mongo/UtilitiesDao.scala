package integration.util.dao.mongo

import altitude.Altitude
import altitude.dao.mongo.{BaseMongoDao}
import altitude.transactions.TransactionId
import integration.IntegrationTestCore

class UtilitiesDao(val app: Altitude) extends BaseMongoDao("") with integration.util.dao.UtilitiesDao {
  override def migrateDatabase(): Unit = {
    BaseMongoDao.DB.get.dropDatabase()
    IntegrationTestCore.mongoDbApp.service.migration.migrate()
  }

  override def close() = Unit
  override def rollback() = Unit
  override def cleanupTest() = Unit
  override def createTransaction(tx: TransactionId) = Unit
}
