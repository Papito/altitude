package software.altitude.core.dao.jdbc

import play.api.libs.json.JsObject
import software.altitude.core.models.{FieldType, MetadataField}
import software.altitude.core.transactions.TransactionId
import software.altitude.core.{Const => C, AltitudeCoreApp, Altitude, Context}

abstract class MetadataFieldDao (val app: AltitudeCoreApp)
  extends BaseJdbcDao("metadata_field") with software.altitude.core.dao.MetadataFieldDao {

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
        INSERT INTO $TABLE_NAME (
             $CORE_SQL_COLS_FOR_INSERT,
             ${C.MetadataField.NAME},
             ${C.MetadataField.NAME_LC},
             ${C.MetadataField.FIELD_TYPE})
            VALUES ($CORE_SQL_VALS_FOR_INSERT, ?, ?, ?)
        """

    val sqlVals: List[Any] = List(
      metadataField.name,
      metadataField.nameLowercase,
      metadataField.fieldType.toString)

    addRecord(jsonIn, sql, sqlVals)
  }
}
