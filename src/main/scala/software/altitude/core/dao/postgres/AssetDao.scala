package software.altitude.core.dao.postgres

import software.altitude.core.AltitudeAppContext
import software.altitude.core.RequestContext
import software.altitude.core.dao.jdbc.BaseDao
import software.altitude.core.models.Metadata
import software.altitude.core.{Const => C}

object AssetDao {
    val DEFAULT_SQL_COLS_FOR_SELECT: List[String] = List(
      "asset.*",
      s"(asset.${C.Asset.METADATA}#>>'{}')::text AS ${C.Asset.METADATA}",
      s"(asset.${C.Asset.EXTRACTED_METADATA}#>>'{}')::text AS ${C.Asset.EXTRACTED_METADATA}",
      BaseDao.totalRecsWindowFunction
    )
}

class AssetDao(app: AltitudeAppContext) extends software.altitude.core.dao.jdbc.AssetDao(app) with Postgres {
  override protected def columnsForSelect: List[String] = AssetDao.DEFAULT_SQL_COLS_FOR_SELECT

  override def getMetadata(assetId: String): Option[Metadata] = {
    val sql = s"""
      SELECT (${C.Asset.METADATA}#>>'{}')::text AS ${C.Asset.METADATA}
         FROM $tableName
       WHERE ${C.Base.REPO_ID} = ? AND ${C.Asset.ID} = ?
      """

    oneBySqlQuery(sql, List(RequestContext.getRepository.id.get, assetId)) match {
      case Some(rec) =>
        Some(getMetadataJsonFromColumn(rec(C.Asset.METADATA)))
      case None => None
    }
  }
}
