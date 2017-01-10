package integration.util.dao

import altitude.transactions.TransactionId

trait UtilitiesDao {
  def migrateDatabase(): Unit
  protected def rollback(): Unit
  protected def close(): Unit
  def cleanupTest(): Unit
  def createTransaction(txId: TransactionId): Unit
}
