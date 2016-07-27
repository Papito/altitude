package altitude.dao

import altitude.models.User
import altitude.transactions.TransactionId

trait StatDao extends BaseDao {
  def incrementStat(statName: String, count: Long = 1)(implicit user: User, txId: TransactionId): Unit
  def decrementStat(statName: String, count: Long = 1)(implicit user: User, txId: TransactionId): Unit = {
    incrementStat(statName, -count)
  }
}
