package altitude.dao.postgres

import altitude.Util.log
import altitude.dao.TransactionId
import altitude.models.{Asset, MediaType}
import altitude.{Const => C}
import org.apache.commons.dbutils.QueryRunner
import play.api.libs.json._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


class LibraryDao extends BasePostgresDao("asset") with altitude.dao.LibraryDao {

  override def add(jsonIn: JsObject)(implicit txId: TransactionId): Future[JsObject] = {
    val asset = jsonIn: Asset

    // Postgres will reject this sequence with jsonb
    val metadata: String = asset.metadata.toString().replaceAll("\\\\u0000", "")

    /*
    Add the asset
     */
    val asset_sql = s"""
        INSERT INTO $tableName (
             $coreSqlColsForInsert, ${C.Asset.PATH}, ${C.Asset.MD5}, ${C.Asset.MEDIA_TYPE},
             ${C.Asset.MEDIA_SUBTYPE}, ${C.Asset.MIME_TYPE}, ${C.Asset.METADATA})
            VALUES($coreSqlValuesForInsert, ?, ?, ?, ?, ?, CAST(? AS jsonb))
    """

    val asset_sql_vals: List[Object] =
      asset.path ::
      asset.md5 ::
      asset.mediaType.mediaType ::
      asset.mediaType.mediaSubtype ::
      asset.mediaType.mime ::
      metadata :: Nil

    addRecord(jsonIn, asset_sql, asset_sql_vals)
  }

  override def getById(id: String)(implicit txId: TransactionId): Future[Option[JsObject]] = {
    log.debug(s"Getting by ID '$id'", C.tag.DB)
    val run: QueryRunner = new QueryRunner()

    val sql =s"""
      SELECT ${C.Base.ID}, *,
             EXTRACT(EPOCH FROM created_at) AS created_at,
             EXTRACT(EPOCH FROM updated_at) AS updated_at
        FROM $tableName
       WHERE ${C.Base.ID} = ?"""

    val optRec = oneBySqlQuery(sql, List(id))

    optRec match {
      case None => Future {None}
      case _ =>
        val rec = optRec.get

        val mediaType = new MediaType(
          mediaType = rec.get(C.Asset.MEDIA_TYPE).get.toString,
          mediaSubtype = rec.get(C.Asset.MEDIA_SUBTYPE).get.toString,
          mime = rec.get(C.Asset.MIME_TYPE).get.toString)

        Future {
          val model = Asset(id = Some(rec.get(C.Asset.ID).get.toString),
            path = rec.get(C.Asset.PATH).get.toString,
            md5 = rec.get(C.Asset.MD5).get.toString,
            mediaType = mediaType,
            metadata = Json.parse(rec.get(C.Asset.METADATA).get.toString))

          addCoreAttrs(model, rec)
          Some(model)
        }
    }
  }
}
