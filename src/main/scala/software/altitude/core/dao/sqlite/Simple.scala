package software.altitude.core.dao.sqlite

import software.altitude.core.AltitudeCoreApp

class FolderDao(val app: AltitudeCoreApp) extends software.altitude.core.dao.jdbc.FolderDao with Sqlite

class MetadataFieldDao(val app: AltitudeCoreApp) extends software.altitude.core.dao.jdbc.MetadataFieldDao with Sqlite

class MigrationDao(val app: AltitudeCoreApp, val SYSTEM_TABLE: String)
  extends software.altitude.core.dao.jdbc.MigrationDao with Sqlite

class RepositoryDao(val app: AltitudeCoreApp) extends software.altitude.core.dao.jdbc.RepositoryDao with Sqlite

class StatDao(val app: AltitudeCoreApp) extends software.altitude.core.dao.jdbc.StatDao with Sqlite {
  override def DEFAULT_SQL_COLS_FOR_SELECT = "*"
}
