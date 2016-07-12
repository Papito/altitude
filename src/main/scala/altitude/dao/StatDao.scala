package altitude.dao

import altitude.transactions.TransactionId

trait StatDao extends BaseDao {
  def incrementStat(statName: String, count: Long = 1)(implicit txId: TransactionId): Unit
  def decrementStat(statName: String, count: Long = 1)(implicit txId: TransactionId): Unit = {
    incrementStat(statName, -count)
  }
}
