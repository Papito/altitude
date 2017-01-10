package altitude.dao.jdbc

import altitude.transactions.TransactionId
import altitude.{Const => C, Context, Altitude}
import altitude.models.Asset
import org.slf4j.LoggerFactory
import play.api.libs.json.{JsObject, Json}

abstract class SearchDao(val app: Altitude) extends BaseJdbcDao("search_token") with altitude.dao.SearchDao {
  private final val log = LoggerFactory.getLogger(getClass)

  override protected def makeModel(rec: Map[String, AnyRef]): JsObject = Json.obj()

  override def indexAsset(asset: Asset)(implicit ctx: Context, txId: TransactionId) = {

    // repository_id, asset_id, field_id, field_value_lc

/*
    val sql = s"""
        INSERT INTO $tableName (
             $CORE_SQL_COLS_FOR_INSERT, ${C.SearchToken.ASSET_ID},
             ${C.SearchToken.FIELD_ID}, ${C.SearchToken.FIELD_VALUE})
            VALUES(
              $CORE_SQL_VALS_FOR_INSERT,
              ?, ?, ?
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
*/
  }
}

