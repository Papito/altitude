package altitude.dao.jdbc

import altitude.transactions.TransactionId
import altitude.{Const => C, Altitude}
import altitude.models.{UserMetadataField, User}
import org.slf4j.LoggerFactory
import play.api.libs.json.{Json, JsString, JsArray, JsObject}

abstract class UserMetadataFieldDao (val app: Altitude)
  extends BaseJdbcDao("metadata_field") with altitude.dao.UserMetadataFieldDao {
  private final val log = LoggerFactory.getLogger(getClass)

  override protected def makeModel(rec: Map[String, AnyRef]): JsObject = {
    val maxLength = rec.get(C("MetadataField.MAX_LENGTH"))

    val model = UserMetadataField(
      id = Some(rec.get(C("Base.ID")).get.asInstanceOf[String]),
      userId = rec.get(C("Base.USER_ID")).get.asInstanceOf[String],
      name = rec.get(C("MetadataField.NAME")).get.asInstanceOf[String],
      fieldType = rec.get(C("MetadataField.FIELD_TYPE")).get.asInstanceOf[String],
      maxLength =  if (maxLength.isDefined) Some(rec.get(C("MetadataField.MAX_LENGTH")).get.asInstanceOf[Int]) else None,
      fixedList = None
    )
    addCoreAttrs(model, rec)
    model
  }

  override def add(jsonIn: JsObject)(implicit user: User,  txId: TransactionId): JsObject = {
    val metadataField = jsonIn: UserMetadataField

    var sql = s"""
        INSERT INTO $tableName (
             $CORE_SQL_COLS_FOR_INSERT, ${C("Base.USER_ID")},
             ${C("MetadataField.NAME")}, ${C("MetadataField.NAME_LC")}, ${C("MetadataField.FIELD_TYPE")},
             ${C("MetadataField.MAX_LENGTH")})
            VALUES ($CORE_SQL_VALS_FOR_INSERT, ?, ?, ?, ?, ?)
    """

    var sqlVals: List[Object] = List(
      metadataField.userId,
      metadataField.name,
      metadataField.nameLowercase,
      metadataField.fieldType,
      if (metadataField.maxLength.isDefined) metadataField.maxLength.get.asInstanceOf[Object] else null)

    val storedField: UserMetadataField = addRecord(jsonIn, sql, sqlVals)

    // now insert fixed values if any
    if (metadataField.fixedList.isDefined) {
      val values = metadataField.fixedList.get
      val valuePlaceholders = values.map{v => "(?, ?)"}

      sql = s"""
          INSERT INTO metadata_field_fixed_list
            (${C("MetadataFixedList.FIELD_ID")}, ${C("MetadataFixedList.LIST_VALUE")})
          VALUES ${valuePlaceholders.mkString(", ")}
         """

      sqlVals = values.foldLeft(List[Object]()) {
        (res, v) => res ::: List(storedField.id.get, v.asInstanceOf[Object])}

      addRecords(sql, sqlVals)
    }

    storedField
  }

  override def getById(id: String)(implicit user: User, txId: TransactionId): Option[JsObject] = {
    log.debug(s"Getting by ID '$id' from '$tableName'", C.LogTag.DB)

    val metadataFieldJson: Option[JsObject] = super.getById(id)

    if (metadataFieldJson.isEmpty) {
      return None
    }

    val SQL = s"""
      SELECT ${C("MetadataFixedList.LIST_VALUE")}
        FROM metadata_field_fixed_list
       WHERE field_id = ?"""
    val recs: List[Map[String, AnyRef]] = manyBySqlQuery(SQL, List(id))

    val fixedListValues: List[String] = recs.map{m =>
      m.get(C("MetadataFixedList.LIST_VALUE")).get.asInstanceOf[String]
    }.sorted

    // set the fixed list values IF ANY to the json object
    fixedListValues.isEmpty match {
      case true => metadataFieldJson
      case false => {
        Some(metadataFieldJson.get ++ JsObject(Seq(
          C("MetadataField.FIXED_LIST") -> Json.toJson(fixedListValues))))
      }
    }
  }
}
