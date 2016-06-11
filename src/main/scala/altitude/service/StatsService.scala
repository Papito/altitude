package altitude.service

import altitude.Altitude
import altitude.dao.StatsDao
import org.slf4j.LoggerFactory
import net.codingwell.scalaguice.InjectorExtensions._

class StatsService(app: Altitude){
  private final val log = LoggerFactory.getLogger(getClass)
  protected val DAO = app.injector.instance[StatsDao]

}

