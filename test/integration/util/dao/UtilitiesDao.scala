package integration.util.dao

import altitude.dao.{TransactionId, BaseDao}

trait UtilitiesDao extends BaseDao {
  def dropDatabase(): Unit
  protected def rollback(): Unit
  protected def close(): Unit
  def cleanup(): Unit
  def createTransaction(tx: TransactionId): Unit
}
