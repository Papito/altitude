package altitude.dao.postgres

import altitude.models.{Asset, AssetType, Metadata}
import altitude.transactions.TransactionId
import altitude.{Altitude, Const => C, Context}
import play.api.libs.json.{JsObject, Json}

class AssetDao(app: Altitude) extends altitude.dao.jdbc.AssetDao(app) with Postgres {
  override protected def DEFAULT_SQL_COLS_FOR_SELECT = s"""
      ${C.Base.ID}, *, (${C.Asset.METADATA}#>>'{}')::text as ${C.Asset.METADATA},
      EXTRACT(EPOCH FROM created_at) AS created_at,
      EXTRACT(EPOCH FROM updated_at) AS updated_at
    """

  override def getMetadata(assetId: String)(implicit ctx: Context, txId: TransactionId): Option[Metadata] = {
    val sql = s"""
      SELECT (${C.Asset.METADATA}#>>'{}')::text as ${C.Asset.METADATA}
         FROM $tableName
       WHERE ${C.Base.REPO_ID} = ? AND ${C.Asset.ID} = ?
      """

    oneBySqlQuery(sql, List(ctx.repo.id.get, assetId)) match {
      case Some(rec) =>
        val metadataJsonStr: String = rec.getOrElse(C.Asset.METADATA, "{}").asInstanceOf[String]
        val metadataJson = Json.parse(metadataJsonStr).as[JsObject]
        val metadata = Metadata.fromJson(metadataJson)
        Some(metadata)
      case None => None
    }
  }
}