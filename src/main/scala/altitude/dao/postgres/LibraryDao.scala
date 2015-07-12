package altitude.dao.postgres

import altitude.models.{Asset, MediaType, Preview}
import altitude.transactions.TransactionId
import altitude.{Altitude, Const => C}
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
      //imageData = rec.get(C.Asset.IMAGE_PREVIEW).get.asInstanceOf[Array[Byte]],
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

  override def addPreview(asset: Asset, bytes: Array[Byte])(implicit txId: TransactionId = new TransactionId): Option[String] = throw new NotImplementedError
  override def getPreview(id: String)(implicit txId: TransactionId = new TransactionId): Option[Preview]= throw new NotImplementedError
}
