package altitude.service

import altitude.{Context, Altitude}
import altitude.dao.StatDao
import altitude.models.search.Query
import altitude.models.{Stat, Stats}
import altitude.transactions.AbstractTransactionManager
import net.codingwell.scalaguice.InjectorExtensions._
import org.slf4j.LoggerFactory

class StatsService(app: Altitude){
  private final val log = LoggerFactory.getLogger(getClass)
  protected val DAO = app.injector.instance[StatDao]
  protected val txManager = app.injector.instance[AbstractTransactionManager]

  def getStats(implicit ctx: Context): Stats = {
    txManager.asReadOnly[Stats] {
      val q = Query(user = ctx.user)
      val allStats: List[Stat] = DAO.query(q).records.map(Stat.fromJson)
      Stats(allStats)
    }
  }

  def incrementStat(statName: String, count: Long = 1)
                   (implicit ctx: Context): Unit = {
    DAO.incrementStat(statName, count)
  }

  def decrementStat(statName: String, count: Long = 1)
                   (implicit ctx: Context): Unit = {
    DAO.decrementStat(statName, count)
  }

  def createStat(dimension: String)
                (implicit ctx: Context) = {
    txManager.withTransaction {
      val stat = Stat(ctx.user.id.get, dimension, 0)
      DAO.add(stat)
    }
  }
}

