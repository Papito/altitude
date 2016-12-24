package altitude.service

import altitude.dao.StatDao
import altitude.models.search.Query
import altitude.models.{Stat, Stats}
import altitude.transactions.{TransactionId, AbstractTransactionManager}
import altitude.{Altitude, Context}
import net.codingwell.scalaguice.InjectorExtensions._
import org.slf4j.LoggerFactory

class StatsService(app: Altitude){
  private final val log = LoggerFactory.getLogger(getClass)
  protected val DAO = app.injector.instance[StatDao]
  protected val txManager = app.injector.instance[AbstractTransactionManager]

  def getStats(implicit ctx: Context, txId: TransactionId = new TransactionId): Stats = {
    txManager.asReadOnly[Stats] {
      val allStats: List[Stat] = DAO.query(Query()).records.map(Stat.fromJson)
      Stats(allStats)
    }
  }

  def incrementStat(statName: String, count: Long = 1)
                   (implicit ctx: Context, txId: TransactionId = new TransactionId): Unit = {
    DAO.incrementStat(statName, count)
  }

  def decrementStat(statName: String, count: Long = 1)
                   (implicit ctx: Context, txId: TransactionId = new TransactionId): Unit = {
    DAO.decrementStat(statName, count)
  }

  def createStat(dimension: String)
                (implicit ctx: Context, txId: TransactionId = new TransactionId) = {
    txManager.withTransaction {
      val stat = Stat(ctx.repo.id.get, dimension, 0)
      DAO.add(stat)
    }
  }
}

