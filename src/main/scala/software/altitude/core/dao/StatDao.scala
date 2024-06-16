package software.altitude.core.dao

import software.altitude.core.Context
import software.altitude.core.dao.jdbc.BaseDao
import software.altitude.core.transactions.TransactionId

trait StatDao extends BaseDao {
  def incrementStat(statName: String, count: Long = 1)(implicit ctx: Context, txId: TransactionId): Unit
  def decrementStat(statName: String, count: Long = 1)(implicit ctx: Context, txId: TransactionId): Unit = {
    incrementStat(statName, -count)
  }
}
