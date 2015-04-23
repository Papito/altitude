package altitude.dao.postgres

import altitude.models.{AssetLocation, MediaType, Asset}
import altitude.dao.TransactionId
import altitude.Util.log
import play.api.libs.json._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import altitude.{Const => C}


class LibraryDao extends BasePostgresDao("asset") with altitude.dao.LibraryDao {

  override def add(jsonIn: JsObject)(implicit txId: TransactionId): Future[JsObject] = {
    val asset = jsonIn: Asset

    // Postgres will reject this sequence with jsonb
    val metadata: String = asset.metadata.toString().replaceAll("\\\\u0000", "")

    val q: String = s"""
        INSERT INTO $tableName (
             $coreSqlColsForInsert, ${C.Asset.MEDIA_TYPE},
             ${C.Asset.MEDIA_SUBTYPE}, ${C.Asset.MIME_TYPE}, ${C.Asset.METADATA})
            VALUES($coreSqlValuesForInsert, ?, ?, ?, CAST(? AS jsonb))
    """

    val values: List[Object] =
      asset.mediaType.mediaType ::
      asset.mediaType.mediaSubtype ::
      asset.mediaType.mime ::
      metadata :: Nil

    addRecord(jsonIn, q, values)
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
