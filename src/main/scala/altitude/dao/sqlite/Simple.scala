package altitude.dao.sqlite

import altitude.Altitude

class AssetDao(app: Altitude) extends altitude.dao.jdbc.AssetDao(app) with Sqlite

class FolderDao(app: Altitude) extends altitude.dao.jdbc.FolderDao(app) with Sqlite

class MetadataFieldDao(app: Altitude) extends altitude.dao.jdbc.MetadataFieldDao(app) with Sqlite

class MigrationDao (app: Altitude) extends altitude.dao.jdbc.MigrationDao(app)

class RepositoryDao(app: Altitude) extends altitude.dao.jdbc.RepositoryDao(app) with Sqlite

class TrashDao(app: Altitude) extends altitude.dao.jdbc.TrashDao(app) with Sqlite

class SearchDao(app: Altitude) extends altitude.dao.jdbc.SearchDao(app) with Sqlite

class StatDao(app: Altitude) extends altitude.dao.jdbc.StatDao(app) with Sqlite {
  override def DEFAULT_SQL_COLS_FOR_SELECT = "*"
}
