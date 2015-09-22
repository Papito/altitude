package altitude.service

import altitude.Altitude
import altitude.dao.MigrationDao
import altitude.transactions.{TransactionId, AbstractTransactionManager}
import org.slf4j.LoggerFactory
import net.codingwell.scalaguice.InjectorExtensions._

class MigratioService(app: Altitude) {
  val log =  LoggerFactory.getLogger(getClass)
  protected val txManager = app.injector.instance[AbstractTransactionManager]
  protected val DAO = app.injector.instance[MigrationDao]
  protected val CURRENT_VERSION = 1

  log.info("Migration service initialized")

  def migrationRequired(implicit txId: TransactionId = new TransactionId): Boolean = {
    val version = txManager.asReadOnly[Int] {
      DAO.currentVersion
    }
    log.info(s"Current database version is @ $version")
    version < CURRENT_VERSION
  }

  def migrate(): Unit = {

  }
}
