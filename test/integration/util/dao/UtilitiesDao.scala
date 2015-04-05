package integration.util.dao

import altitude.dao.{TransactionId, BaseDao}

trait UtilitiesDao extends BaseDao {
  def dropDatabase(): Unit
  def rollback(): Unit
  def close(): Unit
  def createTransaction(tx: TransactionId): Unit
}
