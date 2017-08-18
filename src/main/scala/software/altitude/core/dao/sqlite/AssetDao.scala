package software.altitude.core.dao.sqlite

import software.altitude.core.Altitude

object AssetDao {
  val DEFAULT_SQL_COLS_FOR_SELECT = s"""
      asset.*,
      CAST(STRFTIME('%s', asset.created_at) AS INT) AS created_at,
      CAST(STRFTIME('%s', asset.updated_at) AS INT) AS updated_at
    """
}

class AssetDao(app: Altitude) extends software.altitude.core.dao.jdbc.AssetDao(app) with Sqlite {
  override protected def DEFAULT_SQL_COLS_FOR_SELECT = AssetDao.DEFAULT_SQL_COLS_FOR_SELECT
}

