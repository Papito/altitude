package altitude.service.migration

import altitude.dao.MigrationDao
import altitude.models.{Repository, Stats, User}
import altitude.transactions.{TransactionId, AbstractTransactionManager}
import altitude.{Altitude, Context}
import net.codingwell.scalaguice.InjectorExtensions._
import org.slf4j.LoggerFactory

import scala.io.Source
import altitude.{Const => C, _}

abstract class MigrationService(app: Altitude) {
  private final val log = LoggerFactory.getLogger(getClass)

  protected val DAO: MigrationDao = app.injector.instance[MigrationDao]
  protected val txManager = app.injector.instance[AbstractTransactionManager]

  protected val CURRENT_VERSION = 1
  protected val ROOT_MIGRATIONS_PATH = "/migrations/"
  protected val MIGRATIONS_DIR: String
  protected val FILE_EXTENSION: String

  def runMigration(version: Int)
                  (implicit ctx: Context = new Context(repo = C.REPO, user = C.USER),
                   txId: TransactionId = new TransactionId) = {
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
        case 1 => v1(ctx)
      }

      DAO.versionUp()
    }
  }

  private def v1(context: Context)
                (implicit txId: TransactionId = new TransactionId) = {

    implicit val ctx: Context = new Context(user = C.USER, repo = C.REPO)

    // user "uncategorized" folder node
    val uncatFolder = app.service.folder.getUncatFolder()
    app.service.folder.add(uncatFolder)

    app.service.stats.createStat(Stats.TOTAL_ASSETS)
    app.service.stats.createStat(Stats.TOTAL_BYTES)
    app.service.stats.createStat(Stats.UNCATEGORIZED_ASSETS)
    app.service.stats.createStat(Stats.RECYCLED_ASSETS)
    app.service.stats.createStat(Stats.RECYCLED_BYTES)
  }

  def existingVersion(implicit ctx: Context = new Context(repo = C.REPO, user = C.USER),
                      txId: TransactionId = new TransactionId): Int = {
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

  def migrate() = {
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
