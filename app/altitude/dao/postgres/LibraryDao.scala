package altitude.dao.postgres

import altitude.models.{AssetLocation, MediaType, Asset}
import altitude.dao.TransactionId
import altitude.Util.log
import org.apache.commons.dbutils.QueryRunner
import play.api.libs.json._
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
        (res, el) => (record \ C.Asset.ID).as[String] :: el.locId.toInt.asInstanceOf[Object] :: el.path :: res
      }

      val run: QueryRunner = new QueryRunner()
      log.debug(s"SQL: $location_query. ARGS: ${location_query_values.toString()}")

      run.update(conn, location_query, location_query_values:_*)

      record
    }
  }

  override def getById(id: String)(implicit txId: TransactionId): Future[Option[JsObject]] = {
    val recOpt = getRecordById(id)

    if (recOpt == None) {
      return Future[Option[JsObject]] {None}
    }

    val rec = recOpt.get

    val mediaType = new MediaType(
      mediaType = rec.get(C.Asset.MEDIA_TYPE).get.toString,
      mediaSubtype = rec.get(C.Asset.MEDIA_SUBTYPE).get.toString,
      mime = rec.get(C.Asset.MIME_TYPE).get.toString)

    val locations = List[AssetLocation](
      AssetLocation(locId = "1", path =  ""))

    Future[Option[JsObject]] {
      val model = Asset(id = Some(rec.get(C.Asset.ID).get.toString),
            locations = locations,
            mediaType = mediaType,
            metadata = Json.parse(rec.get(C.Asset.METADATA).get.toString))

      addCoreAttrs(model, rec)
      Some(model)
    }
  }
}
