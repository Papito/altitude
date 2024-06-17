package software.altitude.core.service.migration

import net.codingwell.scalaguice.InjectorExtensions._
import org.slf4j.LoggerFactory
import software.altitude.core.AltitudeAppContext
import software.altitude.core.Environment
import software.altitude.core.dao.MigrationDao
import software.altitude.core.transactions.TransactionManager

import java.io.File
import scala.io.Source

abstract class CoreMigrationService {
  private final val log = LoggerFactory.getLogger(getClass)

  protected val app: AltitudeAppContext
  protected val DAO: MigrationDao = app.injector.instance[MigrationDao]
  protected val txManager: TransactionManager = app.txManager
  protected val CURRENT_VERSION: Int

  protected val MIGRATIONS_DIR: String
  protected val FILE_EXTENSION: String

  def migrateVersion(version: Int): Unit

  private def runMigration(version: Int): Unit = {

    val sqlCommands = parseMigrationCommands(version)
    txManager.withTransaction {
        DAO.executeCommand(sqlCommands)
    }

    // must have schema changes committed
    txManager.withTransaction {
      migrateVersion(version)
      DAO.versionUp()
    }
  }

  def existingVersion: Int = {
    txManager.asReadOnly[Int] {
      DAO.currentVersion
    }
  }

  def migrationRequired: Boolean = {
    log.info("Checking if migration is required")
    val version = existingVersion
    log.info(s"Current database version is @ $version")
    val isRequired = version < CURRENT_VERSION
    log.info(s"Migration required? : $isRequired")
    isRequired
  }

  def migrate(): Unit = {
    val oldVersion = existingVersion
    log.warn("!!!! MIGRATING !!!!")
    log.info(s"From version $oldVersion to $CURRENT_VERSION")
    for (version <- oldVersion + 1 to CURRENT_VERSION) {
      runMigration(version)
    }
  }

  private def parseMigrationCommands(version: Int): String = {
    log.info(s"RUNNING MIGRATION TO VERSION ^^$version^^")

    val entireSchemaPath = new File(MIGRATIONS_DIR, "all.sql").toString

    /* We load the entire schema as the one and only migration in the following cases:
        * 1. In test and dev environments
        * 2. When migrating to version 1 in prod
     */
    val path = Environment.ENV match {
      case Environment.TEST | Environment.DEV =>entireSchemaPath
      case Environment.PROD => if (version == 1) entireSchemaPath
        else
          new File(MIGRATIONS_DIR, s"$version$FILE_EXTENSION").toString
    }

    log.info(s"Migration path: $path")

    val r = getClass.getResource(path)
    val source = Source.fromURL(r)
    val commands = source.mkString
    source.close()

    commands
  }

}
