package integration.util.dao

import altitude.dao.{BaseDao, TransactionId}

trait UtilitiesDao extends BaseDao {
  def dropDatabase(): Unit
  protected def rollback(): Unit
  protected def close(): Unit
  def cleanup(): Unit
  def createTransaction(tx: TransactionId): Unit
}
