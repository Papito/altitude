package altitude.dao.jdbc

import altitude.transactions.TransactionId
import altitude.{Const => C, Altitude}
import altitude.models.{Trash, MediaType}
import org.slf4j.LoggerFactory
import play.api.libs.json.JsObject

abstract class TrashDao(val app: Altitude) extends BaseJdbcDao("trash") with altitude.dao.TrashDao {
  private final val log = LoggerFactory.getLogger(getClass)

  override protected def makeModel(rec: Map[String, AnyRef]): JsObject = {
    val mediaType = new MediaType(
      mediaType = rec.get(C("Asset.MEDIA_TYPE")).get.asInstanceOf[String],
      mediaSubtype = rec.get(C("Asset.MEDIA_SUBTYPE")).get.asInstanceOf[String],
      mime = rec.get(C("Asset.MIME_TYPE")).get.asInstanceOf[String])

    val model = new Trash(
      id = Some(rec.get(C("Base.ID")).get.asInstanceOf[String]),
      path = rec.get(C("Asset.PATH")).get.asInstanceOf[String],
      md5 = rec.get(C("Asset.MD5")).get.asInstanceOf[String],
      mediaType = mediaType,
      sizeBytes = rec.get(C("Asset.SIZE_BYTES")).get.asInstanceOf[Int],
      folderId = rec.get(C("Asset.FOLDER_ID")).get.asInstanceOf[String])

    addCoreAttrs(model, rec)
    model
  }

  override def add(jsonIn: JsObject)(implicit txId: TransactionId): JsObject = {
    println(jsonIn.toString())

    val trash = jsonIn: Trash

    // Postgres will reject this sequence with jsonb
    val metadata: String = trash.metadata.toString().replaceAll("\\\\u0000", "")

    /*
    Add the asset
     */
    val sql = s"""
        INSERT INTO $tableName (
             $CORE_SQL_COLS_FOR_INSERT, ${C("Asset.PATH")}, ${C("Asset.MD5")},
             ${C("Asset.FILENAME")}, ${C("Asset.SIZE_BYTES")},
             ${C("Asset.MEDIA_TYPE")}, ${C("Asset.MEDIA_SUBTYPE")}, ${C("Asset.MIME_TYPE")},
             ${C("Asset.FOLDER_ID")}, ${C("Asset.METADATA")})
            VALUES($CORE_SQL_VALS_FOR_INSERT, ?, ?, ?, ?, ?, ?, ?, ?, $JSON_PLACEHOLDER)
    """

    val sqlVals: List[Object] = List(
      trash.path,
      trash.md5,
      trash.fileName,
      trash.sizeBytes.asInstanceOf[Object],
      trash.mediaType.mediaType,
      trash.mediaType.mediaSubtype,
      trash.mediaType.mime,
      trash.folderId,
      metadata)

    addRecord(jsonIn, sql, sqlVals)
  }
}
