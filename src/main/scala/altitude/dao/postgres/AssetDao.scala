package altitude.dao.postgres

import altitude.models.{BaseModel, Asset, AssetType, Metadata}
import altitude.transactions.TransactionId
import altitude.{Altitude, Const => C, Context}
import org.joda.time.DateTime
import play.api.libs.json.{JsObject, Json}

class AssetDao(app: Altitude) extends altitude.dao.jdbc.AssetDao(app) with Postgres {
  override protected def DEFAULT_SQL_COLS_FOR_SELECT = s"""
      ${C.Base.ID}, *,
      (${C.Asset.METADATA}#>>'{}')::text as ${C.Asset.METADATA},
      (${C.Asset.EXTRACTED_METADATA}#>>'{}')::text as ${C.Asset.EXTRACTED_METADATA},
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
        val metadataCol = rec.get(C.Asset.METADATA).get
        val metadataJsonStr: String = if (metadataCol == null) "{}" else metadataCol.asInstanceOf[String]
        val metadataJson = Json.parse(metadataJsonStr).as[JsObject]
        val metadata = Metadata.fromJson(metadataJson)
        Some(metadata)
      case None => None
    }
  }

  override protected def setRecycledAtProperty(asset: Asset, rec: Map[String, AnyRef]): Asset = {
    val recycledAtMilis = rec.getOrElse(C.Base.CREATED_AT, 0d).asInstanceOf[Double].toLong
    if (recycledAtMilis != 0d) {
      asset.recycledAt = new DateTime(recycledAtMilis * 1000)
    }

    asset
  }


}