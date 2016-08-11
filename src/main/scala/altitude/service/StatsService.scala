package altitude.service

import altitude.Altitude
import altitude.dao.StatDao
import altitude.models.search.Query
import altitude.models.{User, Stat, Stats}
import altitude.transactions.{AbstractTransactionManager, TransactionId}
import org.slf4j.LoggerFactory
import net.codingwell.scalaguice.InjectorExtensions._
import play.api.libs.json.{JsValue, JsObject}

class StatsService(app: Altitude){
  private final val log = LoggerFactory.getLogger(getClass)
  protected val DAO = app.injector.instance[StatDao]
  protected val txManager = app.injector.instance[AbstractTransactionManager]

  def getStats(implicit user: User, txId: TransactionId = new TransactionId): Stats = {
    txManager.asReadOnly[Stats] {
      val q = Query(user = user)
      val allStats: List[Stat] = DAO.query(q).records.map(Stat.fromJson)
      Stats(allStats)
    }
  }

  def incrementStat(statName: String, count: Long = 1)
                   (implicit user: User, txId: TransactionId): Unit = {
    DAO.incrementStat(statName, count)
  }

  def decrementStat(statName: String, count: Long = 1)
                   (implicit user: User, txId: TransactionId): Unit = {
    DAO.decrementStat(statName, count)
  }

  def createStat(dimension: String)
                (implicit user: User, txId: TransactionId = new TransactionId()) = {
    txManager.withTransaction {
      val stat = Stat(user.id.get, dimension, 0)
      DAO.add(stat)
    }
  }
}

