package altitude.dao.sqlite

import altitude.Altitude

class FolderDao(app: Altitude) extends altitude.dao.jdbc.FolderDao(app) with Sqlite

class MetadataFieldDao(app: Altitude) extends altitude.dao.jdbc.MetadataFieldDao(app) with Sqlite

class MigrationDao(app: Altitude) extends altitude.dao.jdbc.MigrationDao(app) with Sqlite

class RepositoryDao(app: Altitude) extends altitude.dao.jdbc.RepositoryDao(app) with Sqlite

class StatDao(app: Altitude) extends altitude.dao.jdbc.StatDao(app) with Sqlite {
  override def DEFAULT_SQL_COLS_FOR_SELECT = "*"
}
