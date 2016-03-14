package altitude.dao.jdbc

//import altitude.models.ImportProfile
import altitude.transactions.TransactionId
import altitude.{Altitude, Const => C}
import org.slf4j.LoggerFactory
import play.api.libs.json.{JsObject, Json}

/*
abstract class ImportProfileDao(val app: Altitude) extends BaseJdbcDao("import_profile") with altitude.dao.ImportProfileDao {
  private final val log = LoggerFactory.getLogger(getClass)

  override protected def makeModel(rec: Map[String, AnyRef]): JsObject = {
    val model = ImportProfile(
      id = Some(rec.get(C.ImportProfile.ID).get.asInstanceOf[String]),
      name = rec.get(C.ImportProfile.NAME).get.asInstanceOf[String],
      tagData = Json.parse(rec.get(C.ImportProfile.TAG_DATA).get.toString))

    addCoreAttrs(model, rec)
    model
  }

  override def add(jsonIn: JsObject)(implicit txId: TransactionId): JsObject = {
    val importProfile = jsonIn: ImportProfile

    val sql = s"""
        INSERT INTO $tableName (
             $CORE_SQL_COLS_FOR_INSERT, ${C.ImportProfile.NAME}, ${C.ImportProfile.TAG_DATA})
            VALUES ($CORE_SQL_VALS_FOR_INSERT, ?, CAST(? AS jsonb))
    """

    val sqlVals: List[Object] = List(
      importProfile.name,
      importProfile.tagData.toString().replaceAll("\\\\u0000", ""))

    addRecord(jsonIn, sql, sqlVals)
  }
}
*/

