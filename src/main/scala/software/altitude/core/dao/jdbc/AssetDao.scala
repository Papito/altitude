package software.altitude.core.dao.jdbc

import org.apache.commons.dbutils.QueryRunner
import org.slf4j.LoggerFactory
import play.api.libs.json._
import software.altitude.core.models.{Asset, AssetType, Metadata}
import software.altitude.core.transactions.TransactionId
import software.altitude.core.util.{Query, QueryResult}
import software.altitude.core.{Const => C, AltitudeCoreApp, Altitude, Context}

abstract class AssetDao(val app: AltitudeCoreApp) extends BaseJdbcDao("asset") with software.altitude.core.dao.AssetDao {
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
      fileName = rec.get(C.Asset.FILENAME).get.asInstanceOf[String],
      isRecycled = rec.get(C.Asset.IS_RECYCLED).get.asInstanceOf[Int] match {
        case 0 => false
        case 1 => true
      },
      md5 = rec.get(C.Asset.MD5).get.asInstanceOf[String],
      assetType = assetType,
      sizeBytes = rec.get(C.Asset.SIZE_BYTES).get.asInstanceOf[Int],
      metadata = metadataJson: Metadata,
      extractedMetadata = extractedMetadataJson: Metadata,
      folderId = rec.get(C.Asset.FOLDER_ID).get.asInstanceOf[String])

    addCoreAttrs(model, rec)
  }

  protected lazy val NOT_RECYCLED_QUERY_BUILDER = new NotRecycledQueryBuilder(DEFAULT_SQL_COLS_FOR_SELECT, TABLE_NAME)
  override def queryNotRecycled(q: Query)(implicit ctx: Context, txId: TransactionId): QueryResult =
    this.query(q, NOT_RECYCLED_QUERY_BUILDER)


  protected lazy val RECYCLED_QUERY_BUILDER = new RecycledQueryBuilder(DEFAULT_SQL_COLS_FOR_SELECT, TABLE_NAME)
  override def queryRecycled(q: Query)(implicit ctx: Context, txId: TransactionId): QueryResult =
    this.query(q, RECYCLED_QUERY_BUILDER)

  override def getMetadata(assetId: String)(implicit ctx: Context, txId: TransactionId): Option[Metadata] = {
    val sql = s"""
      SELECT ${C.Asset.METADATA}
         FROM $TABLE_NAME
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

    val metadata: String = asset.metadata.toString().replaceAll("\\\\u0000", "")
    val extractedMetadata: String = asset.extractedMetadata.toString().replaceAll("\\\\u0000", "")

    val sql = s"""
        INSERT INTO $TABLE_NAME (
             $CORE_SQL_COLS_FOR_INSERT, ${C.Base.USER_ID}, ${C.Asset.MD5},
             ${C.Asset.FILENAME}, ${C.Asset.SIZE_BYTES},
             ${C.AssetType.MEDIA_TYPE}, ${C.AssetType.MEDIA_SUBTYPE}, ${C.AssetType.MIME_TYPE},
             ${C.Asset.FOLDER_ID}, ${C.Asset.METADATA}, ${C.Asset.EXTRACTED_METADATA})
            VALUES( $CORE_SQL_VALS_FOR_INSERT, ?, ?, ?, ?, ?, ?, ?, ?, $JSON_FUNC, $JSON_FUNC)
    """

    val sqlVals: List[Any] = List(
      asset.userId,
      asset.md5,
      asset.fileName,
      asset.sizeBytes.asInstanceOf[Object],
      asset.assetType.mediaType,
      asset.assetType.mediaSubtype,
      asset.assetType.mime,
      asset.folderId,
      metadata,
      extractedMetadata)

    addRecord(jsonIn, sql, sqlVals)
  }

  override def setMetadata(assetId: String, metadata: Metadata)
                          (implicit ctx: Context, txId: TransactionId) = {
    val sql = s"""
      UPDATE $TABLE_NAME
         SET ${C.Base.UPDATED_AT} = $CURRENT_TIME_FUNC, ${C.Asset.METADATA} = $JSON_FUNC
       WHERE ${C.Base.REPO_ID} = ? AND ${C.Asset.ID} = ?
      """

    val updateValues = List(metadata.toString, ctx.repo.id.get, assetId)
    log.debug(s"Update SQL: [$sql] with values: $updateValues")
    val runner: QueryRunner = new QueryRunner()

    runner.update(conn, sql, updateValues:_*)
  }

  override def setAssetAsRecycled(assetId: String, isRecycled: Boolean)
                            (implicit ctx: Context, txId: TransactionId) = {
    val sql = s"""
        UPDATE $TABLE_NAME
           SET ${C.Base.UPDATED_AT} = $CURRENT_TIME_FUNC,
               ${C.Asset.IS_RECYCLED} = ?
         WHERE ${C.Base.REPO_ID} = ? AND ${C.Asset.ID} = ?
      """

    val _isRecycled = if (isRecycled) 1 else 0
    val updateValues = List[Object](_isRecycled.asInstanceOf[Object], ctx.repo.id.get, assetId)
    log.debug(s"Update SQL: [$sql] with values: $updateValues")

    val runner: QueryRunner = new QueryRunner()
    runner.update(conn, sql, updateValues:_*)
  }
}
