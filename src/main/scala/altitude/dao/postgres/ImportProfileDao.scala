package altitude.dao.postgres

import altitude.{Const => C, Altitude}
import altitude.models.{ImportProfile, MediaType}
import altitude.transactions.TransactionId
import play.api.libs.json.{Json, JsObject}

class ImportProfileDao(val app: Altitude) extends BasePostgresDao("asset") with altitude.dao.ImportProfileDao {
  override protected def makeModel(rec: Map[String, AnyRef]): JsObject = {
    val model = ImportProfile(
      id = Some(rec.get(C.ImportProfile.ID).get.asInstanceOf[String]),
      name = rec.get(C.ImportProfile.NAME).get.asInstanceOf[String])

    addCoreAttrs(model, rec)
    model
  }

  override def add(jsonIn: JsObject)(implicit txId: TransactionId): JsObject = {
    val importProfile = jsonIn: ImportProfile

    val sql = s"""
        INSERT INTO $tableName (
             $CORE_SQL_COLS_FOR_INSERT, ${C.ImportProfile.NAME})
            VALUES($CORE_SQL_VALS_FOR_INSERT, ?)
    """

    val sqlVals: List[Object] = List(
      importProfile.name)

    addRecord(jsonIn, sql, sqlVals)
  }
}

