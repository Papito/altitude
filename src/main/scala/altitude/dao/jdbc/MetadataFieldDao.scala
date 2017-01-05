package altitude.dao.jdbc

import altitude.models.{FieldType, MetadataField}
import altitude.transactions.TransactionId
import altitude.{Altitude, Const => C, Context}
import play.api.libs.json.JsObject

abstract class MetadataFieldDao (val app: Altitude)
  extends BaseJdbcDao("metadata_field") with altitude.dao.MetadataFieldDao {

  override protected def makeModel(rec: Map[String, AnyRef]): JsObject = {
    val model = MetadataField(
      id = Some(rec.get(C.Base.ID).get.asInstanceOf[String]),
      name = rec.get(C.MetadataField.NAME).get.asInstanceOf[String],
      fieldType = FieldType.withName(
        rec.get(C.MetadataField.FIELD_TYPE).get.asInstanceOf[String])
    )
    addCoreAttrs(model, rec)
  }

  override def add(jsonIn: JsObject)(implicit ctx: Context, txId: TransactionId): JsObject = {
    val metadataField = jsonIn: MetadataField

    val sql = s"""
        INSERT INTO $tableName (
             $CORE_SQL_COLS_FOR_INSERT,
             ${C.MetadataField.NAME},
             ${C.MetadataField.NAME_LC},
             ${C.MetadataField.FIELD_TYPE})
            VALUES ($CORE_SQL_VALS_FOR_INSERT, ?, ?, ?)
        """

    val sqlVals: List[Object] = List(
      metadataField.name,
      metadataField.nameLowercase,
      metadataField.fieldType.toString)

    addRecord(jsonIn, sql, sqlVals)
  }
}
