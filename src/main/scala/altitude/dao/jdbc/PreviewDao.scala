package altitude.dao.jdbc

import altitude.models.Preview
import altitude.transactions.TransactionId
import altitude.{Altitude, Const => C}
import org.apache.commons.codec.binary.Base64
import org.slf4j.LoggerFactory
import play.api.libs.json._


abstract class PreviewDao(val app: Altitude) extends BaseJdbcDao("preview") with altitude.dao.PreviewDao {
  private final val log = LoggerFactory.getLogger(getClass)

  protected def makeModel(rec: Map[String, AnyRef]): JsObject = Json.obj()

  override def add(json: JsObject)(implicit txId: TransactionId): JsObject = {
    val preview: Preview = json

    val preview_sql = s"""
        INSERT INTO preview (
             $CORE_SQL_COLS_FOR_INSERT, ${C.Preview.ASSET_ID},
             ${C.Preview.MIME_TYPE}, ${C.Preview.DATA})
            VALUES($CORE_SQL_VALS_FOR_INSERT, ?, ?, ?)
    """

    val base64EncodedData = Base64.encodeBase64String(preview.data)
    val preview_sql_vals: List[Object] = List(
      preview.asset_id,
      preview.mime_type,
      base64EncodedData)

    addRecord(preview, preview_sql, preview_sql_vals)
  }

  override def getById(asset_id: String)(implicit txId: TransactionId): Option[JsObject] = {
    log.debug(s"Getting preview for asset id '$asset_id'")

    val sql = s"""
      SELECT $DEFAULT_SQL_COLS_FOR_SELECT
        FROM ${this.tableName}
       WHERE ${C.Preview.ASSET_ID} = ?"""

    val rec: Option[Map[String, AnyRef]] = oneBySqlQuery(sql, List(asset_id))

    val data: Array[Byte] = Base64.decodeBase64(rec.get(C.Preview.DATA).asInstanceOf[String])
    val preview = Preview(
      id=Some(rec.get(C.Preview.ID).asInstanceOf[String]),
      asset_id=rec.get(C.Preview.ASSET_ID).asInstanceOf[String],
      mime_type=rec.get(C.Preview.MIME_TYPE).asInstanceOf[String],
      data=data
    )
    addCoreAttrs(preview, rec.get)
    Some(preview)
  }
}
