package software.altitude.core.service.migration

import org.slf4j.LoggerFactory
import software.altitude.core.{AltitudeCoreApp, Altitude}

trait JdbcMigrationService {
  val app: AltitudeCoreApp

  private final val log = LoggerFactory.getLogger(getClass)
  val FILE_EXTENSION = ".sql"
  log.info("JDBC migration service initialized")
}
