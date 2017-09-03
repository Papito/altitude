package software.altitude.core.dao.postgres

import software.altitude.core.{Const => C, AltitudeCoreApp}
import software.altitude.core.dao.jdbc

class RepositoryDao(app: AltitudeCoreApp) extends jdbc.RepositoryDao(app) with Postgres {
  override protected def DEFAULT_SQL_COLS_FOR_SELECT = s"""
      *,
      (repository.${C.Repository.FILES_STORE_CONFIG}#>>'{}')::text as ${C.Repository.FILES_STORE_CONFIG},
      EXTRACT(EPOCH FROM created_at) AS created_at,
      EXTRACT(EPOCH FROM updated_at) AS updated_at
    """
}
