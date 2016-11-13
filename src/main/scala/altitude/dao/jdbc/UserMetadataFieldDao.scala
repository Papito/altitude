package altitude.dao.jdbc

import altitude.transactions.TransactionId
import altitude.{Const => C, Altitude}
import altitude.models.{MetadataField, User}
import org.slf4j.LoggerFactory
import play.api.libs.json.JsObject

abstract class UserMetadataFieldDao (val app: Altitude)
  extends BaseJdbcDao("metadata_field") with altitude.dao.UserMetadataFieldDao {
  private final val log = LoggerFactory.getLogger(getClass)

  override protected def makeModel(rec: Map[String, AnyRef]): JsObject = {
    val model = MetadataField(
      id = Some(rec.get(C("Base.ID")).get.asInstanceOf[String]),
      userId = rec.get(C("Base.USER_ID")).get.asInstanceOf[String],
      name = rec.get(C("MetadataField.NAME")).get.asInstanceOf[String],
      fieldType = rec.get(C("MetadataField.FIELD_TYPE")).get.asInstanceOf[String],
      isFixedList = rec.get(C("MetadataField.IS_FIXED_LIST")).get.asInstanceOf[Boolean],
      maxLength = Some(rec.get(C("MetadataField.IS_FIXED_LIST")).get.asInstanceOf[Int])
    )
    addCoreAttrs(model, rec)
    model
  }

  override def add(jsonIn: JsObject)(implicit user: User,  txId: TransactionId): JsObject = {
    val metadataField = jsonIn: MetadataField

    val sql = s"""
        INSERT INTO $tableName (
             $CORE_SQL_COLS_FOR_INSERT, ${C("Base.USER_ID")},
             ${C("MetadataField.NAME")}, ${C("MetadataField.NAME_LC")}, ${C("MetadataField.FIELD_TYPE")},
             ${C("MetadataField.IS_FIXED_LIST")}, ${C("IS_FIXED_LIST.NAME")})
            VALUES ($CORE_SQL_VALS_FOR_INSERT, ?, ?, ?, ?, ?, ?)
    """

    val sqlVals: List[Object] = List(
      metadataField.userId,
      metadataField.name,
      metadataField.nameLowercase,
      metadataField.fieldType,
      metadataField.isFixedList.asInstanceOf[Object],
      metadataField.maxLength.asInstanceOf[Object])

    addRecord(jsonIn, sql, sqlVals)
  }
}
