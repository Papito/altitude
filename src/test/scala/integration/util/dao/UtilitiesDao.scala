package integration.util.dao

import altitude.Altitude
import altitude.dao.BaseDao
import altitude.transactions.TransactionId

trait UtilitiesDao extends BaseDao {
  val app: Altitude
  def dropDatabase(): Unit
  protected def rollback(): Unit
  protected def close(): Unit
  def cleanup(): Unit
  def createTransaction(tx: TransactionId): Unit
}