package altitude.dao.postgres

import altitude.models.{Asset, MediaType, Preview}
import altitude.transactions.TransactionId
import altitude.{Altitude, Const => C}
import org.apache.commons.codec.binary.Base64
import play.api.libs.json._


class LibraryDao(val app: Altitude) extends BasePostgresDao("asset") with altitude.dao.LibraryDao {

  override protected def makeModel(rec: Map[String, AnyRef]): JsObject = {
    val mediaType = new MediaType(
      mediaType = rec.get(C.Asset.MEDIA_TYPE).get.asInstanceOf[String],
      mediaSubtype = rec.get(C.Asset.MEDIA_SUBTYPE).get.asInstanceOf[String],
      mime = rec.get(C.Asset.MIME_TYPE).get.asInstanceOf[String])

    val model = Asset(id = Some(rec.get(C.Asset.ID).get.asInstanceOf[String]),
      path = rec.get(C.Asset.PATH).get.asInstanceOf[String],
      md5 = rec.get(C.Asset.MD5).get.asInstanceOf[String],
      mediaType = mediaType,
      sizeBytes = rec.get(C.Asset.SIZE_BYTES).get.asInstanceOf[Long],
      metadata = Json.parse(rec.get(C.Asset.METADATA).get.toString))

    addCoreAttrs(model, rec)
    log.debug(model.toJson.toString())
    model
  }

  override def add(jsonIn: JsObject)(implicit txId: TransactionId): JsObject = {
    val asset = jsonIn: Asset

    // Postgres will reject this sequence with jsonb
    val metadata: String = asset.metadata.toString().replaceAll("\\\\u0000", "")

    /*
    Add the asset
     */
    val asset_sql = s"""
        INSERT INTO $tableName (
             $coreSqlColsForInsert, ${C.Asset.PATH}, ${C.Asset.MD5},
             ${C.Asset.FILENAME}, ${C.Asset.SIZE_BYTES},
             ${C.Asset.MEDIA_TYPE}, ${C.Asset.MEDIA_SUBTYPE}, ${C.Asset.MIME_TYPE},
             ${C.Asset.METADATA})
            VALUES($coreSqlValuesForInsert, ?, ?, ?, ?, ?, ?, ?, CAST(? AS jsonb))
    """

    val asset_sql_vals: List[Object] = List(
      asset.path,
      asset.md5,
      asset.fileName,
      asset.sizeBytes.asInstanceOf[Object],
      asset.mediaType.mediaType,
      asset.mediaType.mediaSubtype,
      asset.mediaType.mime,
      metadata)

    addRecord(jsonIn, asset_sql, asset_sql_vals)
  }

  override def addPreview(asset: Asset, bytes: Array[Byte])
                         (implicit txId: TransactionId = new TransactionId): Option[String] = {
    require(asset.id.nonEmpty)
    if (bytes.length < 0) return None

    log.info(s"Saving preview for ${asset.path}")

    val preview: Preview = Preview(asset_id=asset.id.get, mime_type=asset.mediaType.mime, data=bytes)

    val preview_sql = s"""
        INSERT INTO preview (
             $coreSqlColsForInsert, ${C.Preview.ASSET_ID},
             ${C.Preview.MIME_TYPE}, ${C.Preview.DATA})
            VALUES($coreSqlValuesForInsert, ?, ?, ?)
    """

    val base64EncodedData = Base64.encodeBase64String(bytes)
    val preview_sql_vals: List[Object] = List(
      preview.asset_id,
      preview.mime_type,
      base64EncodedData)

    addRecord(preview, preview_sql, preview_sql_vals)

    Some(base64EncodedData)
  }

  override def getPreview(asset_id: String)
                         (implicit txId: TransactionId = new TransactionId): Option[Preview] = {
    log.debug(s"Getting preview for asset id '$asset_id'")

    val sql = s"""
      SELECT ${C.Preview.ID}, *,
             EXTRACT(EPOCH FROM created_at) AS created_at,
             EXTRACT(EPOCH FROM updated_at) AS updated_at
        FROM preview
       WHERE ${C.Preview.ASSET_ID} = ?"""

    val rec: Option[Map[String, AnyRef]] = oneBySqlQuery(sql, List(asset_id))

    val data: Array[Byte] = Base64.decodeBase64(rec.get(C.Preview.DATA).asInstanceOf[String])
    val preview = Preview(
      id=Some(rec.get(C.Preview.ID).asInstanceOf[String]),
      asset_id=rec.get(C.Preview.ASSET_ID).asInstanceOf[String],
      mime_type=rec.get(C.Preview.MIME_TYPE).asInstanceOf[String],
      data=data
    )
    addCoreAttrs(preview, rec.get)
    Some(preview)
  }
}
