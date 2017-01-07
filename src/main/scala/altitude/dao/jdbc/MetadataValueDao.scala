package altitude.dao.jdbc

import altitude.models.MetadataValue
import altitude.transactions.TransactionId
import altitude.{Altitude, Const => C, Context}
import play.api.libs.json.JsObject

abstract class MetadataValueDao (val app: Altitude)
  extends BaseJdbcDao("metadata_values") with altitude.dao.MetadataValueDao {

  override protected def makeModel(rec: Map[String, AnyRef]): JsObject = {
    val model = MetadataValue(
      id = Some(rec.get(C.Base.ID).get.asInstanceOf[String]),
      fieldId = rec.get(C.MetadataValue.FIELD_ID).get.asInstanceOf[String],
      assetId = rec.get(C.MetadataValue.ASSET_ID).get.asInstanceOf[String],
      value = rec.get(C.MetadataValue.FIELD_VALUE).get.asInstanceOf[String]
    )
    addCoreAttrs(model, rec)
  }

  override def add(jsonIn: JsObject)(implicit ctx: Context, txId: TransactionId): JsObject = {
    val metadataValue = jsonIn: MetadataValue

    val sql = s"""
        INSERT INTO $tableName (
             $CORE_SQL_COLS_FOR_INSERT,
             ${C.MetadataValue.FIELD_ID},
             ${C.MetadataValue.ASSET_ID},
             ${C.MetadataValue.FIELD_VALUE},
             ${C.MetadataValue.FIELD_VALUE_LC})
            VALUES ($CORE_SQL_VALS_FOR_INSERT, ?, ?, ?, ?)
        """

    val sqlVals: List[Object] = List(
      metadataValue.fieldId,
      metadataValue.assetId,
      metadataValue.value,
      metadataValue.valueLowerCase)

    addRecord(jsonIn, sql, sqlVals)
  }
}

