package software.altitude.core.dao.postgres

import software.altitude.core.AltitudeCoreApp

class FolderDao(val app: AltitudeCoreApp) extends software.altitude.core.dao.jdbc.FolderDao with Postgres

class RepositoryDao(val app: AltitudeCoreApp) extends software.altitude.core.dao.jdbc.RepositoryDao with Postgres

class MigrationDao(val app: AltitudeCoreApp, val SYSTEM_TABLE: String)
  extends software.altitude.core.dao.jdbc.MigrationDao with Postgres

class MetadataFieldDao(val app: AltitudeCoreApp) extends software.altitude.core.dao.jdbc.MetadataFieldDao with Postgres

class StatDao(val app: AltitudeCoreApp) extends software.altitude.core.dao.jdbc.StatDao with Postgres {
  override def DEFAULT_SQL_COLS_FOR_SELECT = "*"
}
