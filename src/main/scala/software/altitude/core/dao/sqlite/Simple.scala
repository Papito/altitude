package software.altitude.core.dao.sqlite

import software.altitude.core.AltitudeCoreApp

class FolderDao(app: AltitudeCoreApp) extends software.altitude.core.dao.jdbc.FolderDao(app) with Sqlite

class MetadataFieldDao(app: AltitudeCoreApp) extends software.altitude.core.dao.jdbc.MetadataFieldDao(app) with Sqlite

class MigrationDao(app: AltitudeCoreApp, systemTable: String)
  extends software.altitude.core.dao.jdbc.MigrationDao(app, systemTable) with Sqlite

class RepositoryDao(app: AltitudeCoreApp) extends software.altitude.core.dao.jdbc.RepositoryDao(app) with Sqlite

class StatDao(val app: AltitudeCoreApp) extends software.altitude.core.dao.jdbc.StatDao with Sqlite {
  override def DEFAULT_SQL_COLS_FOR_SELECT = "*"
}
