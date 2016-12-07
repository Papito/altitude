package altitude.dao.jdbc

import altitude.models.search.{Query, QueryResult}
import altitude.models.{User, UserMetadataField}
import altitude.transactions.TransactionId
import altitude.{Altitude, Const => C}
import org.slf4j.LoggerFactory
import play.api.libs.json.{JsObject, Json}

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
      constraintList = None
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

    // now insert constraint values if any
    if (metadataField.constraintList.isDefined) {
      val values = metadataField.constraintList.get
      val valuePlaceholders = values.map{v => "(?, ?)"}

      sql = s"""
          INSERT INTO constraint_value
            (${C("MetadataConstraintValue.FIELD_ID")}, ${C("MetadataConstraintValue.CONSTRAINT_VALUE")})
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
      SELECT ${C("MetadataConstraintValue.CONSTRAINT_VALUE")}
        FROM constraint_value
       WHERE field_id = ?"""
    val recs: List[Map[String, AnyRef]] = manyBySqlQuery(SQL, List(id))

    val constraintValues: List[String] = recs.map{m =>
      m.get(C("MetadataConstraintValue.CONSTRAINT_VALUE")).get.asInstanceOf[String]
    }.sorted

    // set the constraint values IF ANY to the json object
    constraintValues.isEmpty match {
      case true => metadataFieldJson
      case false => Some(metadataFieldJson.get ++ JsObject(Seq(
          C("MetadataField.CONSTRAINT_LIST") -> Json.toJson(constraintValues))))
    }
  }

  final val CONSTRAINT_VALS_SQL_QUERY_BUILDER =
    new SqlQueryBuilder(C("MetadataConstraintValue.CONSTRAINT_VALUE"), "constraint_value")

  override def query(q: Query)
                    (implicit user: User, txId: TransactionId): QueryResult = {
    // first, get the fields themselves
    val fieldResults: QueryResult = super.query(q)

    if (fieldResults.isEmpty) {
      return fieldResults
    }

    val fieldIds: List[String] = fieldResults.records.map{json => (json \ C("Base.ID")).as[String]}

    val SQL = s"""
      SELECT ${C("MetadataConstraintValue.FIELD_ID")}, ${C("MetadataConstraintValue.CONSTRAINT_VALUE")}
        FROM constraint_value
       WHERE field_id
       IN (${makeSqlPlaceholders(fieldIds)})"""
    val recs: List[Map[String, AnyRef]] = manyBySqlQuery(SQL, fieldIds)

    /* Group constraint values by their parent field.

       Right now the result is a mess of options and maps

       Map(
          Some(FIELD_ID) -> List(
                              Map(field_id -> FIELD_ID, constraint_value -> VALUE1),
                              Map(field_id -> FIELD_ID, constraint_value -> VALUE2))
       )
    */
    val constraintValLookup: Map[String, List[String]] = recs.groupBy(
        // partition into a map where the key is field id - half way there
        _.get(C("MetadataConstraintValue.FIELD_ID")).get).map{
        // massage the values of the resulting map into a neat array of string (right now it's a list of maps)
        case (fieldId, values) =>
          val valuesAsList = values.map{ v =>
            v.get(C("MetadataConstraintValue.CONSTRAINT_VALUE")).get.asInstanceOf[String]
          }
          // boom
          (fieldId.toString, valuesAsList.sorted)
    }

    /* We have a clean lookup map of constraint values - as a list, where the key is the field id.
       This is easy and fast to join as the final result.
    */
    val records = fieldResults.records.map{metadataFieldJson =>
      // get the id of the metadata field to match on
      val id = (metadataFieldJson \ C("Base.ID")).as[String]
      val constraintValues: List[String] = constraintValLookup.getOrElse(id, List())

      // this is identical to what we do in getById()
      constraintValues.isEmpty match {
        case true => metadataFieldJson
        case false => metadataFieldJson ++ JsObject(Seq(
            C("MetadataField.CONSTRAINT_LIST") -> Json.toJson(constraintValues)))
      }
    }

    QueryResult(
      records = records,
      total = fieldResults.total,
      query= fieldResults.query)
  }

}
