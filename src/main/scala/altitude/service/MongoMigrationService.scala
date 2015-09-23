package altitude.service

import altitude.Altitude
import altitude.transactions.TransactionId
import org.slf4j.LoggerFactory

class MongoMigrationService(app: Altitude) extends MigrationService {
  private val log =  LoggerFactory.getLogger(getClass)

  val EVOLUTIONS_DIR = "mongo/"
  val FILE_EXTENSION = ".mongo"

  protected def runMigration(version: Int): Unit = Unit
  protected def existingVersion(implicit txId: TransactionId = new TransactionId): Int = 1

  log.info("MONGODB migration service initialized")
}
