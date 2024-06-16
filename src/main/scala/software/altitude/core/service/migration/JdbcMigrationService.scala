package software.altitude.core.service.migration

import org.slf4j.LoggerFactory
import software.altitude.core.AltitudeAppContext

trait JdbcMigrationService {
  val app: AltitudeAppContext

  private final val log = LoggerFactory.getLogger(getClass)
  val FILE_EXTENSION = ".sql"
  log.info("JDBC migration service initialized")
}
