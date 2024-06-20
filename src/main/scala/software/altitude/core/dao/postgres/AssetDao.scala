package software.altitude.core.dao.postgres

import play.api.libs.json.JsObject
import play.api.libs.json.Json
import software.altitude.core.AltitudeAppContext
import software.altitude.core.RequestContext
import software.altitude.core.models.Metadata
import software.altitude.core.{Const => C}

object AssetDao {
    val DEFAULT_SQL_COLS_FOR_SELECT: List[String] = List(
      "asset.*",
      s"(asset.${C.Asset.METADATA}#>>'{}')::text as ${C.Asset.METADATA}",
      s"(asset.${C.Asset.EXTRACTED_METADATA}#>>'{}')::text as ${C.Asset.EXTRACTED_METADATA}",
    )
}

class AssetDao(app: AltitudeAppContext) extends software.altitude.core.dao.jdbc.AssetDao(app) with Postgres {
  override protected def selectColumns: List[String] = AssetDao.DEFAULT_SQL_COLS_FOR_SELECT

  override def getMetadata(assetId: String): Option[Metadata] = {
    val sql = s"""
      SELECT (${C.Asset.METADATA}#>>'{}')::text as ${C.Asset.METADATA}
         FROM $tableName
       WHERE ${C.Base.REPO_ID} = ? AND ${C.Asset.ID} = ?
      """

    oneBySqlQuery(sql, List(RequestContext.getRepository.id.get, assetId)) match {
      case Some(rec) =>
        val metadataCol = rec(C.Asset.METADATA)
        val metadataJsonStr: String = if (metadataCol == null) "{}" else metadataCol.asInstanceOf[String]
        val metadataJson = Json.parse(metadataJsonStr).as[JsObject]
        val metadata = Metadata.fromJson(metadataJson)
        Some(metadata)
      case None => None
    }
  }
}
