package software.altitude.core.dao.postgres

import software.altitude.core.{AltitudeCoreApp, Altitude}

class FolderDao(app: Altitude) extends software.altitude.core.dao.jdbc.FolderDao(app) with Postgres

class RepositoryDao(app: Altitude) extends software.altitude.core.dao.jdbc.RepositoryDao(app) with Postgres

class MigrationDao(app: AltitudeCoreApp) extends software.altitude.core.dao.jdbc.MigrationDao(app) with Postgres

class MetadataFieldDao(app: Altitude) extends software.altitude.core.dao.jdbc.MetadataFieldDao(app) with Postgres

class StatDao(app: Altitude) extends software.altitude.core.dao.jdbc.StatDao(app) with Postgres {
  override def DEFAULT_SQL_COLS_FOR_SELECT = "*"
}
