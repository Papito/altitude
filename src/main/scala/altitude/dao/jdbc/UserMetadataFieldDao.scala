package altitude.dao.jdbc

import altitude.models.UserMetadataField
import altitude.models.search.{Query, QueryResult}
import altitude.transactions.TransactionId
import altitude.{Altitude, Const => C, Context}
import org.apache.commons.dbutils.QueryRunner
import org.slf4j.LoggerFactory
import play.api.libs.json.{JsObject, Json}

abstract class UserMetadataFieldDao (val app: Altitude)
  extends BaseJdbcDao("metadata_field") with altitude.dao.UserMetadataFieldDao {
  private final val log = LoggerFactory.getLogger(getClass)

  override protected def makeModel(rec: Map[String, AnyRef]): JsObject = {
    val maxLength = rec.get(C.MetadataField.MAX_LENGTH).get

    val model = UserMetadataField(
      id = Some(rec.get(C.Base.ID).get.asInstanceOf[String]),
      name = rec.get(C.MetadataField.NAME).get.asInstanceOf[String],
      fieldType = rec.get(C.MetadataField.FIELD_TYPE).get.asInstanceOf[String],
      maxLength =  if (maxLength != null) Some(maxLength.asInstanceOf[Int]) else None
    )
    addCoreAttrs(model, rec)
  }

  override def add(jsonIn: JsObject)(implicit ctx: Context, txId: TransactionId): JsObject = {
    val metadataField = jsonIn: UserMetadataField

    val sql = s"""
        INSERT INTO $tableName (
             $CORE_SQL_COLS_FOR_INSERT,
             ${C.MetadataField.NAME}, ${C.MetadataField.NAME_LC}, ${C.MetadataField.FIELD_TYPE},
             ${C.MetadataField.MAX_LENGTH})
            VALUES ($CORE_SQL_VALS_FOR_INSERT, ?, ?, ?, ?)
        """

    val sqlVals: List[Object] = List(
      metadataField.name,
      metadataField.nameLowercase,
      metadataField.fieldType,
      if (metadataField.maxLength.isDefined) metadataField.maxLength.get.asInstanceOf[Object] else null)

    addRecord(jsonIn, sql, sqlVals)
  }
}
