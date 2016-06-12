package altitude.dao

import altitude.transactions.TransactionId

trait StatDao extends BaseDao {
  def incrementStat(statName: String, count: Int = 1)(implicit txId: TransactionId): Unit
  def decrementStat(statName: String, count: Int = 1)(implicit txId: TransactionId): Unit = {
    incrementStat(statName, -count)
  }
}
