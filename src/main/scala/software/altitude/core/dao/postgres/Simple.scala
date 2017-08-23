package software.altitude.core.dao.postgres

import software.altitude.core.AltitudeCoreApp

class FolderDao(app: AltitudeCoreApp) extends software.altitude.core.dao.jdbc.FolderDao(app) with Postgres

class RepositoryDao(app: AltitudeCoreApp) extends software.altitude.core.dao.jdbc.RepositoryDao(app) with Postgres

class MigrationDao(app: AltitudeCoreApp, systemTable: String)
  extends software.altitude.core.dao.jdbc.MigrationDao(app, systemTable) with Postgres

class MetadataFieldDao(app: AltitudeCoreApp) extends software.altitude.core.dao.jdbc.MetadataFieldDao(app) with Postgres

class StatDao(val app: AltitudeCoreApp) extends software.altitude.core.dao.jdbc.StatDao with Postgres {
  override def DEFAULT_SQL_COLS_FOR_SELECT = "*"
}
