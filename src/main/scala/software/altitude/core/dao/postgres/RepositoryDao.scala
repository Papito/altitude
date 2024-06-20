package software.altitude.core.dao.postgres

import software.altitude.core.AltitudeAppContext
import software.altitude.core.dao.jdbc
import software.altitude.core.{Const => C}

class RepositoryDao(app: AltitudeAppContext) extends jdbc.RepositoryDao(app) with Postgres {
  override protected def selectColumns: List[String] = List(
    "*",
    s"(repository.${C.Repository.FILES_STORE_CONFIG}#>>'{}')::text as ${C.Repository.FILES_STORE_CONFIG}",
    "EXTRACT(EPOCH FROM created_at) AS created_at",
    "EXTRACT(EPOCH FROM updated_at) AS updated_at"
  )
}
