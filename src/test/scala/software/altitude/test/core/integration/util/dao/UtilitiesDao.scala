package software.altitude.test.core.integration.util.dao

import software.altitude.core.transactions.TransactionId

trait UtilitiesDao {
  def migrateDatabase(): Unit
  protected def rollback(): Unit
  protected def close(): Unit
  def cleanupTest(): Unit
  def createTransaction(txId: TransactionId): Unit
}
