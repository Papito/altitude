package altitude.service.migration

import altitude.Altitude
import org.slf4j.LoggerFactory


class MongoMigrationService(app: Altitude) extends MigrationService(app) {
  private final val log = LoggerFactory.getLogger(getClass)

  log.info("MONGODB migration service initialized")

  val MIGRATIONS_DIR = "mongo/"
  val FILE_EXTENSION = ".mongo"
}
