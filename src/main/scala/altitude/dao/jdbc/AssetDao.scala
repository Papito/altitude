package altitude.dao.jdbc

import altitude.models.{Asset, MediaType}
import altitude.transactions.TransactionId
import altitude.{Altitude, Const => C}
import play.api.libs.json._


class AssetDao(val app: Altitude) extends BaseJdbcDao("asset") with altitude.dao.AssetDao {

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
    model
  }

  override def add(jsonIn: JsObject)(implicit txId: TransactionId): JsObject = {
    val asset = jsonIn: Asset

    // Postgres will reject this sequence with jsonb
    val metadata: String = asset.metadata.toString().replaceAll("\\\\u0000", "")

    /*
    Add the asset
     */
    val sql = s"""
        INSERT INTO $tableName (
             $CORE_SQL_COLS_FOR_INSERT, ${C.Asset.PATH}, ${C.Asset.MD5},
             ${C.Asset.FILENAME}, ${C.Asset.SIZE_BYTES},
             ${C.Asset.MEDIA_TYPE}, ${C.Asset.MEDIA_SUBTYPE}, ${C.Asset.MIME_TYPE},
             ${C.Asset.METADATA})
            VALUES($CORE_SQL_VALS_FOR_INSERT, ?, ?, ?, ?, ?, ?, ?, CAST(? AS jsonb))
    """

    val sqlVals: List[Object] = List(
      asset.path,
      asset.md5,
      asset.fileName,
      asset.sizeBytes.asInstanceOf[Object],
      asset.mediaType.mediaType,
      asset.mediaType.mediaSubtype,
      asset.mediaType.mime,
      metadata)

    addRecord(jsonIn, sql, sqlVals)
  }
}
