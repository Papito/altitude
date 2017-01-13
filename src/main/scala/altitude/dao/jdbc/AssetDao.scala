package altitude.dao.jdbc

import altitude.models.{Asset, AssetType, Metadata}
import altitude.transactions.TransactionId
import altitude.{Altitude, Const => C, Context}
import org.apache.commons.dbutils.QueryRunner
import org.slf4j.LoggerFactory
import play.api.libs.json._

abstract class AssetDao(val app: Altitude) extends BaseJdbcDao("asset") with altitude.dao.AssetDao {
  private final val log = LoggerFactory.getLogger(getClass)

  override protected def makeModel(rec: Map[String, AnyRef]): JsObject = {
    val assetType = new AssetType(
      mediaType = rec.get(C.AssetType.MEDIA_TYPE).get.asInstanceOf[String],
      mediaSubtype = rec.get(C.AssetType.MEDIA_SUBTYPE).get.asInstanceOf[String],
      mime = rec.get(C.AssetType.MIME_TYPE).get.asInstanceOf[String])

    val metadataCol = rec.get(C.Asset.METADATA).get
    val metadataJsonStr: String = if (metadataCol == null) "{}" else metadataCol.asInstanceOf[String]
    val metadataJson = Json.parse(metadataJsonStr).as[JsObject]

    val extractedMetadataCol = rec.get(C.Asset.EXTRACTED_METADATA).get
    val extractedMetadataJsonStr: String =
      if (extractedMetadataCol == null) "{}" else extractedMetadataCol.asInstanceOf[String]
    val extractedMetadataJson = Json.parse(extractedMetadataJsonStr).as[JsObject]

    val model = new Asset(
      id = Some(rec.get(C.Base.ID).get.asInstanceOf[String]),
      userId = rec.get(C.Base.USER_ID).get.asInstanceOf[String],
      path = rec.get(C.Asset.PATH).get.asInstanceOf[String],
      md5 = rec.get(C.Asset.MD5).get.asInstanceOf[String],
      assetType = assetType,
      sizeBytes = rec.get(C.Asset.SIZE_BYTES).get.asInstanceOf[Int],
      metadata = metadataJson: Metadata,
      extractedMetadata = extractedMetadataJson: Metadata,
      folderId = rec.get(C.Asset.FOLDER_ID).get.asInstanceOf[String])

    addCoreAttrs(model, rec)
  }

  override def getMetadata(assetId: String)(implicit ctx: Context, txId: TransactionId): Option[Metadata] = {
    val sql = s"""
      SELECT ${C.Asset.METADATA}
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

  override def add(jsonIn: JsObject)(implicit ctx: Context, txId: TransactionId): JsObject = {
    val asset = jsonIn: Asset

    // Postgres will reject this sequence with jsonb
    val extracetedMetadata: String = asset.extractedMetadata.toString().replaceAll("\\\\u0000", "")

    val sql = s"""
        INSERT INTO $tableName (
             $CORE_SQL_COLS_FOR_INSERT, ${C.Base.USER_ID}, ${C.Asset.PATH}, ${C.Asset.MD5},
             ${C.Asset.FILENAME}, ${C.Asset.SIZE_BYTES},
             ${C.AssetType.MEDIA_TYPE}, ${C.AssetType.MEDIA_SUBTYPE}, ${C.AssetType.MIME_TYPE},
             ${C.Asset.FOLDER_ID}, ${C.Asset.EXTRACTED_METADATA})
            VALUES( $CORE_SQL_VALS_FOR_INSERT, ?, ?, ?, ?, ?, ?, ?, ?, ?, $JSON_FUNC)
    """

    val sqlVals: List[Object] = List(
      asset.userId,
      asset.path,
      asset.md5,
      asset.fileName,
      asset.sizeBytes.asInstanceOf[Object],
      asset.assetType.mediaType,
      asset.assetType.mediaSubtype,
      asset.assetType.mime,
      asset.folderId,
      extracetedMetadata)

    addRecord(jsonIn, sql, sqlVals)
  }

  override def setMetadata(assetId: String, metadata: Metadata)
                          (implicit ctx: Context, txId: TransactionId) = {
    val sql = s"""
      UPDATE $tableName
         SET ${C.Base.UPDATED_AT} = $CURRENT_TIME_FUNC, ${C.Asset.METADATA} = $JSON_FUNC
       WHERE ${C.Base.REPO_ID} = ? AND ${C.Asset.ID} = ?
      """

    val updateValues = List(metadata.toString, ctx.repo.id.get, assetId)
    log.debug(s"Update SQL: [$sql] with values: $updateValues")
    val runner: QueryRunner = new QueryRunner()

    runner.update(conn, sql, updateValues:_*)
  }
}
