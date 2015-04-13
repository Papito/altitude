package altitude.dao.postgres

import altitude.models.{BaseModel, StoreLocation, MediaType, Asset}
import altitude.{Const => C}
import altitude.dao.TransactionId
import altitude.util.log
import org.apache.commons.dbutils.QueryRunner
import org.apache.commons.dbutils.handlers.MapListHandler
import org.joda.time.{DateTime, DateTimeZone}
import play.api.libs.json._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import altitude.{Const => C}


class LibraryDao extends BasePostgresDao("asset") with altitude.dao.LibraryDao {

  override def add(json: JsObject)(implicit txId: TransactionId): Future[JsObject] = {
    val asset = json: Asset

    log.info(s"POSTGRES ASSET INSERT: $asset", C.tag.DB)
    val run: QueryRunner = new QueryRunner

    // Postgres will reject this sequence with jsonb
    val metadata: String = asset.metadata.toString().replaceAll("\\\\u0000", "")

    val q: String = s"""
        INSERT INTO $tableName (
             ${C.Asset.ID}, ${C.Asset.MEDIA_TYPE},
             ${C.Asset.MEDIA_SUBTYPE}, ${C.Asset.MIME_TYPE}, ${C.Asset.METADATA})
            VALUES(?, ?, ?, ?, CAST(? AS jsonb))
    """

    val id = BaseModel.genId

    run.update(conn, q,
      id,
      asset.mediaType.mediaType,
      asset.mediaType.mediaSubtype,
      asset.mediaType.mime,
      metadata)

    Future[JsObject] {
      json
    }
  }

  override def getById(id: String)(implicit txId: TransactionId): Future[JsObject] = {
    log.debug(s"Getting by ID '$id'", C.tag.DB)
    val run: QueryRunner = new QueryRunner()

    val q: String = s"""
        SELECT ${C.Asset.ID}, ${C.Asset.MEDIA_TYPE},
               ${C.Asset.MEDIA_SUBTYPE}, ${C.Asset.MIME_TYPE}, ${C.Asset.METADATA}
          FROM $tableName
         WHERE id = ?
    """

    val res = run.query(conn, q, new MapListHandler(), id)

    log.debug(s"Found ${res.size()} records", C.tag.DB)
    if (res.size() == 0)
      return Future[JsObject](Json.obj())

    if (res.size() > 1)
      throw new Exception("getById should return only a single result")

    val rec = res.get(0)

    val mediaType = new MediaType(
      mediaType = rec.get(C.Asset.MEDIA_TYPE).toString,
      mediaSubtype = rec.get(C.Asset.MEDIA_SUBTYPE).toString,
      mime = rec.get(C.Asset.MIME_TYPE).toString
    )

    val locations = List[StoreLocation](
      StoreLocation(storageId = "1", path =  "")
    )

    Future[JsObject] {
      Asset(id = Some(rec.get(C.Asset.ID).toString),
            locations = locations,
            mediaType = mediaType,
            metadata = Json.parse(rec.get(C.Asset.METADATA).toString))
    }
  }
}
