package altitude.service.migration

import altitude.transactions.TransactionId
import org.slf4j.LoggerFactory

import scala.io.Source

abstract class MigrationService {
  private final val log = LoggerFactory.getLogger(getClass)

  protected val CURRENT_VERSION = 1
  protected val ROOT_MIGRATIONS_PATH = "/migrations/"
  protected val MIGRATIONS_DIR: String
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
      migrate()
    }
  }

  //FIXME: must come from DB
  def migrationConfirmed = false

  def migrate(): Unit = {
    val oldVersion = existingVersion
    log.warn("!!!! MIGRATING !!!!")
    log.info(s"From version $oldVersion to $CURRENT_VERSION")
    for (version <- oldVersion + 1 to CURRENT_VERSION) runMigration(version)
  }

  def parseMigrationCommands(version: Int): List[String] = {
    log.info(s"RUNNING MIGRATION TO VERSION $version")
    val path = s"$ROOT_MIGRATIONS_PATH$MIGRATIONS_DIR$version$FILE_EXTENSION"
    val r = getClass.getResource(path)
    Source.fromURL(r).mkString.split(";").toList
  }

}