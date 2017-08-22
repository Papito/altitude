package software.altitude.core.service.migration

import net.codingwell.scalaguice.InjectorExtensions._
import org.slf4j.LoggerFactory
import software.altitude.core.dao.MigrationDao
import software.altitude.core.models.Stats
import software.altitude.core.transactions.{AbstractTransactionManager, TransactionId}
import software.altitude.core.{Altitude, Const => C, Context}

import scala.io.Source

abstract class MigrationService(app: Altitude) extends CoreMigrationService(app) {
  private final val log = LoggerFactory.getLogger(getClass)

  override protected val ROOT_MIGRATIONS_PATH = "/migrations/"

  override def migrateVersion(ctx: Context, version: Int)(implicit txId: TransactionId = new TransactionId): Unit = {
      version match {
        case 1 => v1(ctx)
      }
  }

  private def v1(context: Context)
                (implicit txId: TransactionId = new TransactionId) = {

    implicit val ctx: Context = new Context(user = app.USER, repo = app.REPO)

    val triageFolder = app.service.folder.getTriageFolder
    app.service.folder.add(triageFolder)

    app.service.stats.createStat(Stats.SORTED_ASSETS)
    app.service.stats.createStat(Stats.SORTED_BYTES)
    app.service.stats.createStat(Stats.TRIAGE_ASSETS)
    app.service.stats.createStat(Stats.TRIAGE_BYTES)
    app.service.stats.createStat(Stats.RECYCLED_ASSETS)
    app.service.stats.createStat(Stats.RECYCLED_BYTES)
  }
}
