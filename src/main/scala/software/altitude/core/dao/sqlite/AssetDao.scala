package software.altitude.core.dao.sqlite

import software.altitude.core.AltitudeAppContext

object AssetDao {
  val DEFAULT_SQL_COLS_FOR_SELECT: List[String] = List(
    "asset.*",
    "CAST(STRFTIME('%s', asset.created_at) AS INT) AS created_at",
    "CAST(STRFTIME('%s', asset.updated_at) AS INT) AS updated_at"
  )
}

class AssetDao(app: AltitudeAppContext) extends software.altitude.core.dao.jdbc.AssetDao(app) with Sqlite {
  override protected def selectColumns: List[String] = AssetDao.DEFAULT_SQL_COLS_FOR_SELECT
}
