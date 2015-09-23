package altitude.service

import altitude.transactions.TransactionId
import org.slf4j.LoggerFactory

abstract class MigrationService {
  private val log =  LoggerFactory.getLogger(getClass)

  protected val CURRENT_VERSION = 1

  protected val ROOT_EVOLUTIONS_PATH = "/evolutions/"
  protected val EVOLUTIONS_DIR: String
  protected val FILE_EXTENSION: String

  protected def runMigration(version: Int): Unit
  protected def existingVersion(implicit txId: TransactionId = new TransactionId): Int

  def migrationRequired(implicit txId: TransactionId = new TransactionId): Boolean = {
    log.info("Checking if migration is required")
    val version = existingVersion
    log.info(s"Current database version is @ $version")
    val isRequired = version < CURRENT_VERSION
    log.info(s"Migration required? : $isRequired")
    isRequired
  }

  def initDb(): Unit = {
    if (existingVersion == 0) {
      log.warn("NEW DATABASE. FORCING MIGRATION")
      migrate(0)
    }
  }

  def migrate(oldVersion: Int): Unit = {
    log.warn("!!!! MIGRATING !!!!")
    log.info(s"From version $oldVersion to $CURRENT_VERSION")
    for (version <- oldVersion + 1 to CURRENT_VERSION) runMigration(version)
  }

}
