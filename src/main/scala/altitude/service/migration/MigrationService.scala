package altitude.service.migration

import altitude.Altitude
import altitude.dao.MigrationDao
import altitude.models.Folder
import altitude.transactions.{AbstractTransactionManager, TransactionId}
import net.codingwell.scalaguice.InjectorExtensions._
import org.slf4j.LoggerFactory

import scala.io.Source

abstract class MigrationService(app: Altitude) {
  private final val log = LoggerFactory.getLogger(getClass)

  protected val DAO: MigrationDao = app.injector.instance[MigrationDao]
  protected val txManager = app.injector.instance[AbstractTransactionManager]

  protected val CURRENT_VERSION = 1
  protected val ROOT_MIGRATIONS_PATH = "/migrations/"
  protected val MIGRATIONS_DIR: String
  protected val FILE_EXTENSION: String

  def runMigration(version: Int)(implicit txId: TransactionId = new TransactionId): Unit = {
    val migrationCommands = parseMigrationCommands(version)

    txManager.withTransaction {
      for (command <- migrationCommands) {
        log.info(s"Executing $command")
        DAO.executeCommand(command)
      }
    }

    // must have schema changes committed
    txManager.withTransaction {
      version match {
        case 1 => v1
      }

      DAO.versionUp()
    }
  }

  private def v1(implicit txId: TransactionId = new TransactionId): Unit = {
    app.service.folder.add(Folder.UNCATEGORIZED)
  }

  def existingVersion(implicit txId: TransactionId = new TransactionId): Int = {
    txManager.asReadOnly[Int] {
      DAO.currentVersion
    }
  }

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

  //FIXME: placeholder, not DB-backed
  def migrationConfirmed = false

  def migrate(): Unit = {
    val oldVersion = existingVersion
    log.warn("!!!! MIGRATING !!!!")
    log.info(s"From version $oldVersion to $CURRENT_VERSION")
    for (version <- oldVersion + 1 to CURRENT_VERSION) {
      runMigration(version)
    }
  }

  def parseMigrationCommands(version: Int): List[String] = {
    log.info(s"RUNNING MIGRATION TO VERSION $version")
    val path = s"$ROOT_MIGRATIONS_PATH$MIGRATIONS_DIR$version$FILE_EXTENSION"
    val r = getClass.getResource(path)
    Source.fromURL(r).mkString.split(";").map(_.trim).toList.filter(_.nonEmpty)
  }

}
