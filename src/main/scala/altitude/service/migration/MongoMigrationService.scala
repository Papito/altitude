package altitude.service.migration

import altitude.Altitude
import altitude.dao.mongo.BaseMongoDao
import altitude.transactions.{AbstractTransactionManager, VoidTransactionManager, TransactionId}
import com.mongodb.DBObject
import org.slf4j.LoggerFactory
import net.codingwell.scalaguice.InjectorExtensions._


class MongoMigrationService(app: Altitude) extends MigrationService(app) {
  private final val log = LoggerFactory.getLogger(getClass)

  log.info("MONGODB migration service initialized")

  val MIGRATIONS_DIR = "mongo/"
  val FILE_EXTENSION = ".mongo"
}
