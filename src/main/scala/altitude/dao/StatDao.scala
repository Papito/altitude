package altitude.dao

import altitude.Context
import altitude.transactions.TransactionId

trait StatDao extends BaseDao {
  def incrementStat(statName: String, count: Long = 1)(implicit ctx: Context, txId: TransactionId): Unit
  def decrementStat(statName: String, count: Long = 1)(implicit ctx: Context, txId: TransactionId): Unit = {
    incrementStat(statName, -count)
  }
}
