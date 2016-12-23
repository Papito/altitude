package altitude.dao.jdbc

import altitude.models.{Asset, AssetType}
import altitude.{Altitude, Const => C, Context}
import org.slf4j.LoggerFactory
import play.api.libs.json._


abstract class AssetDao(val app: Altitude) extends BaseJdbcDao("asset") with altitude.dao.AssetDao {
  private final val log = LoggerFactory.getLogger(getClass)

  override protected def makeModel(rec: Map[String, AnyRef]): JsObject = {
    val assetType = new AssetType(
      mediaType = rec.get(C.AssetType.MEDIA_TYPE).get.asInstanceOf[String],
      mediaSubtype = rec.get(C.AssetType.MEDIA_SUBTYPE).get.asInstanceOf[String],
      mime = rec.get(C.AssetType.MIME_TYPE).get.asInstanceOf[String])

    val model = new Asset(
      id = Some(rec.get(C.Base.ID).get.asInstanceOf[String]),
      userId = rec.get(C.Base.USER_ID).get.asInstanceOf[String],
      repoId = rec.get(C.Base.REPO_ID).get.asInstanceOf[String],
      path = rec.get(C.Asset.PATH).get.asInstanceOf[String],
      md5 = rec.get(C.Asset.MD5).get.asInstanceOf[String],
      assetType = assetType,
      sizeBytes = rec.get(C.Asset.SIZE_BYTES).get.asInstanceOf[Int],
      folderId = rec.get(C.Asset.FOLDER_ID).get.asInstanceOf[String])

    addCoreAttrs(model, rec)
    model
  }

  override def add(jsonIn: JsObject)(implicit ctx: Context): JsObject = {
    val asset = jsonIn: Asset

    // Postgres will reject this sequence with jsonb
    val metadata: String = asset.metadata.toString().replaceAll("\\\\u0000", "")

    /*
    Add the asset
     */
    val sql = s"""
        INSERT INTO $tableName (
             $CORE_SQL_COLS_FOR_INSERT, ${C.Base.USER_ID}, ${C.Asset.PATH}, ${C.Asset.MD5},
             ${C.Asset.FILENAME}, ${C.Asset.SIZE_BYTES},
             ${C.AssetType.MEDIA_TYPE}, ${C.AssetType.MEDIA_SUBTYPE}, ${C.AssetType.MIME_TYPE},
             ${C.Asset.FOLDER_ID}, ${C.Asset.METADATA})
            VALUES(
              $CORE_SQL_VALS_FOR_INSERT,
              ?, ?, ?, ?, ?, ?, ?, ?, ?,
      $JSON_FUNC)
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
      metadata)

    addRecord(jsonIn, sql, sqlVals)
  }
}
