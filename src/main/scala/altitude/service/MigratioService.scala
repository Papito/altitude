package altitude.service

import altitude.Altitude
import altitude.dao.MigrationDao
import org.slf4j.LoggerFactory
import net.codingwell.scalaguice.InjectorExtensions._

class MigratioService(app: Altitude) {
  val log =  LoggerFactory.getLogger(getClass)
  protected val DAO = app.injector.instance[MigrationDao]

  log.info("Migration service initialized")

  def checkCurrentVersion(): Unit = {
    val version = DAO.currentVersion
    log.info(s"Current database version is @ $version")
  }
}
