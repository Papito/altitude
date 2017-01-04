package altitude.dao.postgres

import altitude.models.Metadata
import altitude.transactions.TransactionId
import altitude.{Const => C, Context, Altitude}
import play.api.libs.json.{JsObject, Json}

class AssetDao(app: Altitude) extends altitude.dao.jdbc.AssetDao(app) with Postgres {

  override def getMetadata(assetId: String)(implicit ctx: Context, txId: TransactionId): Option[Metadata] = {
    val sql = s"""
      SELECT (${C.Asset.METADATA}#>>'{}')::text as ${C.Asset.METADATA}
         FROM $tableName
       WHERE ${C.Base.REPO_ID} = ? AND ${C.Asset.ID} = ?
      """

    oneBySqlQuery(sql, List(ctx.repo.id.get, assetId)) match {
      case Some(rec) =>
        val metadataJson: String = rec.getOrElse(C.Asset.METADATA, "{}").asInstanceOf[String]
        val json = Json.parse(metadataJson).as[JsObject]
        val metadata = Metadata.fromJson(json)
        Some(metadata)
      case None => None
    }
  }
}