package software.altitude.core.service.migration

import org.slf4j.LoggerFactory
import software.altitude.core.transactions.TransactionId
import software.altitude.core.{Altitude, Context}

abstract class MigrationService(val app: Altitude) extends CoreMigrationService {
  protected final val log = LoggerFactory.getLogger(getClass)

  def migrateVersion(ctx: Context, version: Int)(implicit txId: TransactionId = new TransactionId): Unit = {
      version match {
        case 1 => v1(ctx)
      }
  }

  private def v1(context: Context)(implicit txId: TransactionId = new TransactionId): Unit = {
  }
}
