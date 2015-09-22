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
    log.info("Checking if migration is required")
    val version = currentVersion
    log.info(s"Current database version is @ $version")
    val isRequired =version < CURRENT_VERSION
    log.info(s"Migration required? : $isRequired")
    isRequired
  }

  def initDb(): Unit = {
    if (currentVersion == 0) {
      log.warn("NEW DATABASE. FORCING MIGRATION")
      migrate(0)
    }
  }

  def migrate(oldVersion: Int): Unit = {
    log.warn("!!!! MIGRATING !!!!")
    log.info(s"From version $oldVersion to $CURRENT_VERSION")
  }

  protected def currentVersion(implicit txId: TransactionId = new TransactionId): Int = {
    val version = txManager.asReadOnly[Int] {
      DAO.currentVersion
    }
    log.info(s"Database is version $version")
    version
  }

}
