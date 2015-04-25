package altitude.dao.postgres

import altitude.models.{AssetLocation, MediaType, Asset}
import altitude.dao.TransactionId
import altitude.Util.log
import org.apache.commons.dbutils.QueryRunner
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import altitude.{Const => C}
import play.api.libs.json._


class LibraryDao extends BasePostgresDao("asset") with altitude.dao.LibraryDao {

  override def add(jsonIn: JsObject)(implicit txId: TransactionId): Future[JsObject] = {
    val asset = jsonIn: Asset

    // Postgres will reject this sequence with jsonb
    val metadata: String = asset.metadata.toString().replaceAll("\\\\u0000", "")

    /*
    Add the asset
     */
    val asset_query = s"""
        INSERT INTO $tableName (
             $coreSqlColsForInsert, ${C.Asset.MEDIA_TYPE},
             ${C.Asset.MEDIA_SUBTYPE}, ${C.Asset.MIME_TYPE}, ${C.Asset.METADATA})
            VALUES($coreSqlValuesForInsert, ?, ?, ?, CAST(? AS jsonb))
    """

    val asset_query_values: List[Object] =
      asset.mediaType.mediaType ::
      asset.mediaType.mediaSubtype ::
      asset.mediaType.mime ::
      metadata :: Nil

    val fRecord = addRecord(jsonIn, asset_query, asset_query_values)

    fRecord map {record =>
      /*
      Add locations (add a placeholder set for each location)
       */
      val placeholders = List.fill(asset.locations.size)("(?, ?, ?)").mkString(",")
      val location_query = s"""
        INSERT INTO asset_location (asset_id, storage_id, path)
             VALUES $placeholders
        """

      val location_query_values: List[Object] = asset.locations.foldLeft(List[Object]()) {
        (res, el) => (record \ C.Asset.ID).as[String] :: el.locId.asInstanceOf[Object] :: el.path :: res
      }

      val run: QueryRunner = new QueryRunner()
      log.debug(s"SQL: $location_query. ARGS: ${location_query_values.toString()}")

      run.update(conn, location_query, location_query_values:_*)

      record
    }
  }

  override def getById(id: String)(implicit txId: TransactionId): Future[Option[JsObject]] = {
    log.debug(s"Getting by ID '$id'", C.tag.DB)
    val run: QueryRunner = new QueryRunner()

    // FIXME: this only assumes one supported storage for now
    val q =s"""
      SELECT ${C.Base.ID}, *,
             EXTRACT(EPOCH FROM a.created_at) AS created_at,
             EXTRACT(EPOCH FROM a.updated_at) AS updated_at
        FROM $tableName as a, asset_location al
       WHERE ${C.Base.ID} = ? AND a.id = al.asset_id"""

    val optRec = oneBySqlQuery(q, List(id))

    optRec match {
      case None => Future {None}
      case _ =>
        val rec = optRec.get

        val mediaType = new MediaType(
          mediaType = rec.get(C.Asset.MEDIA_TYPE).get.toString,
          mediaSubtype = rec.get(C.Asset.MEDIA_SUBTYPE).get.toString,
          mime = rec.get(C.Asset.MIME_TYPE).get.toString)

        val locations = List[AssetLocation](
          AssetLocation(
            locId = rec.get(C.AssetLocation.STORAGE_ID).get.asInstanceOf[Int],
            path =  rec.get(C.AssetLocation.PATH).get.toString))

        Future {
          val model = Asset(id = Some(rec.get(C.Asset.ID).get.toString),
            locations = locations,
            mediaType = mediaType,
            metadata = Json.parse(rec.get(C.Asset.METADATA).get.toString))

          addCoreAttrs(model, rec)
          Some(model)
        }
    }
  }
}
