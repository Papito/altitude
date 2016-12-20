package altitude.dao

import altitude.Context

trait StatDao extends BaseDao {
  def incrementStat(statName: String, count: Long = 1)(implicit ctx: Context): Unit
  def decrementStat(statName: String, count: Long = 1)(implicit ctx: Context): Unit = {
    incrementStat(statName, -count)
  }
}
