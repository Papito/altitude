package software.altitude.core.dao.sqlite

import software.altitude.core.Altitude

class FolderDao(app: Altitude) extends software.altitude.core.dao.jdbc.FolderDao(app) with Sqlite

class MetadataFieldDao(app: Altitude) extends software.altitude.core.dao.jdbc.MetadataFieldDao(app) with Sqlite

class MigrationDao(app: Altitude) extends software.altitude.core.dao.jdbc.MigrationDao(app) with Sqlite

class RepositoryDao(app: Altitude) extends software.altitude.core.dao.jdbc.RepositoryDao(app) with Sqlite

class StatDao(app: Altitude) extends software.altitude.core.dao.jdbc.StatDao(app) with Sqlite {
  override def DEFAULT_SQL_COLS_FOR_SELECT = "*"
}
