package software.altitude.core.service.migration

import software.altitude.core.Altitude
import org.slf4j.LoggerFactory

abstract class JdbcMigrationService(app: Altitude) extends MigrationService(app) {
  private final val log = LoggerFactory.getLogger(getClass)

  val FILE_EXTENSION = ".sql"

  log.info("JDBC migration service initialized")
}
