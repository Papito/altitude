package altitude.dao.postgres

import altitude.models.{BaseModel, Asset, AssetType, Metadata}
import altitude.transactions.TransactionId
import altitude.{Altitude, Const => C, Context}
import org.joda.time.DateTime
import play.api.libs.json.{JsObject, Json}

object AssetDao {
    val DEFAULT_SQL_COLS_FOR_SELECT = s"""
      asset.*,
      (asset.${C.Asset.METADATA}#>>'{}')::text as ${C.Asset.METADATA},
      (asset.${C.Asset.EXTRACTED_METADATA}#>>'{}')::text as ${C.Asset.EXTRACTED_METADATA},
      EXTRACT(EPOCH FROM asset.created_at) AS created_at,
      EXTRACT(EPOCH FROM asset.updated_at) AS updated_at
    """
}

class AssetDao(app: Altitude) extends altitude.dao.jdbc.AssetDao(app) with Postgres {
  override protected def DEFAULT_SQL_COLS_FOR_SELECT = AssetDao.DEFAULT_SQL_COLS_FOR_SELECT

  override def getMetadata(assetId: String)(implicit ctx: Context, txId: TransactionId): Option[Metadata] = {
    val sql = s"""
      SELECT (${C.Asset.METADATA}#>>'{}')::text as ${C.Asset.METADATA}
         FROM $tableName
       WHERE ${C.Base.REPO_ID} = ? AND ${C.Asset.ID} = ?
      """

    oneBySqlQuery(sql, List(ctx.repo.id.get, assetId)) match {
      case Some(rec) =>
        val metadataCol = rec.get(C.Asset.METADATA).get
        val metadataJsonStr: String = if (metadataCol == null) "{}" else metadataCol.asInstanceOf[String]
        val metadataJson = Json.parse(metadataJsonStr).as[JsObject]
        val metadata = Metadata.fromJson(metadataJson)
        Some(metadata)
      case None => None
    }
  }
}