package altitude.service

import altitude.Altitude
import altitude.dao.StatDao
import altitude.models.{Stat, Stats}
import altitude.transactions.{AbstractTransactionManager, TransactionId}
import org.slf4j.LoggerFactory
import net.codingwell.scalaguice.InjectorExtensions._
import play.api.libs.json.{JsValue, JsObject}

class StatsService(app: Altitude){
  private final val log = LoggerFactory.getLogger(getClass)
  protected val DAO = app.injector.instance[StatDao]
  protected val txManager = app.injector.instance[AbstractTransactionManager]

  def getStats(implicit txId: TransactionId): Stats = {
    txManager.asReadOnly[Stats] {
      val allStats: List[Stat]  = DAO.getAll.map(Stat.fromJson)
      Stats(allStats)
    }
  }

  def incrementStat(statName: String, count: Int = 1)(implicit txId: TransactionId): Unit = {
    DAO.incrementStat(statName, count)
  }

  def decrementStat(statName: String, count: Int = 1)(implicit txId: TransactionId): Unit = {
    DAO.decrementStat(statName, count)
  }
}

