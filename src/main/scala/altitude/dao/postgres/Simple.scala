package altitude.dao.postgres

import altitude.Altitude
import play.api.libs.json.{Json, JsNull, JsObject}

class FolderDao(app: Altitude) extends altitude.dao.jdbc.FolderDao(app) with Postgres

class RepositoryDao(app: Altitude) extends altitude.dao.jdbc.RepositoryDao(app) with Postgres

class MigrationDao (app: Altitude) extends altitude.dao.jdbc.MigrationDao(app)

class MetadataFieldDao(app: Altitude) extends altitude.dao.jdbc.MetadataFieldDao(app) with Postgres

class TrashDao(app: Altitude) extends altitude.dao.jdbc.TrashDao(app) with Postgres

class SearchDao(app: Altitude) extends altitude.dao.jdbc.SearchDao(app) with Postgres 

class StatDao(app: Altitude) extends altitude.dao.jdbc.StatDao(app) with Postgres {
  override def DEFAULT_SQL_COLS_FOR_SELECT = "*"
}
