package software.altitude.core.dao.jdbc

import org.apache.commons.dbutils.QueryRunner
import org.slf4j.LoggerFactory
import play.api.libs.json._
import software.altitude.core.models.{Asset, AssetType, Metadata}
import software.altitude.core.transactions.TransactionId
import software.altitude.core.util.{Query, QueryResult}
import software.altitude.core.{AltitudeCoreApp, Context, Const => C}

abstract class AssetDao(val app: AltitudeCoreApp) extends BaseJdbcDao with software.altitude.core.dao.AssetDao {
  private final val log = LoggerFactory.getLogger(getClass)

  override final val tableName = "asset"

  override protected def makeModel(rec: Map[String, AnyRef]): JsObject = {
    val assetType = new AssetType(
      mediaType = rec(C.AssetType.MEDIA_TYPE).asInstanceOf[String],
      mediaSubtype = rec(C.AssetType.MEDIA_SUBTYPE).asInstanceOf[String],
      mime = rec(C.AssetType.MIME_TYPE).asInstanceOf[String])

    val metadataCol = rec(C.Asset.METADATA)
    val metadataJsonStr: String = if (metadataCol == null) "{}" else metadataCol.asInstanceOf[String]
    val metadataJson = Json.parse(metadataJsonStr).as[JsObject]

    val extractedMetadataCol = rec(C.Asset.EXTRACTED_METADATA)
    val extractedMetadataJsonStr: String =
      if (extractedMetadataCol == null) "{}" else extractedMetadataCol.asInstanceOf[String]
    val extractedMetadataJson = Json.parse(extractedMetadataJsonStr).as[JsObject]

    val model = new Asset(
      id = Some(rec(C.Base.ID).asInstanceOf[String]),
      userId = rec(C.Base.USER_ID).asInstanceOf[String],
      fileName = rec(C.Asset.FILENAME).asInstanceOf[String],
      isRecycled = rec(C.Asset.IS_RECYCLED).asInstanceOf[Int] match {
        case 0 => false
        case 1 => true
      },
      checksum = rec(C.Asset.CHECKSUM).asInstanceOf[String],
      assetType = assetType,
      sizeBytes = rec(C.Asset.SIZE_BYTES).asInstanceOf[Int],
      metadata = metadataJson: Metadata,
      extractedMetadata = extractedMetadataJson: Metadata,
      folderId = rec(C.Asset.FOLDER_ID).asInstanceOf[String])

    addCoreAttrs(model, rec)
  }

  protected lazy val QUERY_BUILDER = new SqlQueryBuilder(defaultSqlColsForSelect, tableName)

  override def queryNotRecycled(q: Query)(implicit ctx: Context, txId: TransactionId): QueryResult =
    this.query(q.add(C.Asset.IS_RECYCLED -> false), QUERY_BUILDER)

  override def queryRecycled(q: Query)(implicit ctx: Context, txId: TransactionId): QueryResult =
    this.query(q.add(C.Asset.IS_RECYCLED -> true), QUERY_BUILDER)

  override def queryAll(q: Query)(implicit ctx: Context, txId: TransactionId): QueryResult =
    this.query(q, QUERY_BUILDER)

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

    val metadataWithIds = Metadata.withIds(asset.metadata)

    val metadata: String = metadataWithIds.toString().replaceAll("\\\\u0000", "")
    val extractedMetadata: String = asset.extractedMetadata.toString().replaceAll("\\\\u0000", "")

    val sql = s"""
        INSERT INTO $tableName (
             $coreSqlColsForInsert, ${C.Base.USER_ID}, ${C.Asset.CHECKSUM},
             ${C.Asset.FILENAME}, ${C.Asset.SIZE_BYTES},
             ${C.AssetType.MEDIA_TYPE}, ${C.AssetType.MEDIA_SUBTYPE}, ${C.AssetType.MIME_TYPE},
             ${C.Asset.FOLDER_ID}, ${C.Asset.METADATA}, ${C.Asset.EXTRACTED_METADATA})
            VALUES( $coreSqlValsForInsert, ?, ?, ?, ?, ?, ?, ?, ?, $jsonFunc, $jsonFunc)
    """

    val sqlVals: List[Any] = List(
      asset.userId,
      asset.checksum,
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
                          (implicit ctx: Context, txId: TransactionId): Unit = {

    val metadataWithIds = Metadata.withIds(metadata)

    val sql = s"""
      UPDATE $tableName
         SET ${C.Base.UPDATED_AT} = $nowTimeFunc, ${C.Asset.METADATA} = $jsonFunc
       WHERE ${C.Base.REPO_ID} = ? AND ${C.Asset.ID} = ?
      """

    val updateValues = List(metadataWithIds.toString, ctx.repo.id.get, assetId)
    log.debug(s"Update SQL: [$sql] with values: $updateValues")
    val runner: QueryRunner = new QueryRunner()

    runner.update(conn, sql, updateValues: _*)
  }
}
