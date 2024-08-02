package software.altitude.core.dao.sqlite

import com.typesafe.config.Config
import play.api.libs.json.JsObject
import play.api.libs.json.Json
import software.altitude.core.RequestContext
import software.altitude.core.dao.jdbc.BaseDao
import software.altitude.core.models.Person
import software.altitude.core.{Const => C}

class PersonDao(override val config: Config)
  extends BaseDao
    with software.altitude.core.dao.PersonDao
    with SqliteOverrides {

  override final val tableName = "person"

  override protected def makeModel(rec: Map[String, AnyRef]): JsObject = {
    val newMergedWithIdsList = if (rec(C.Person.MERGED_WITH_IDS) != null) {
      val newMergedWithIdsJson = rec(C.Person.MERGED_WITH_IDS).asInstanceOf[String]
      getListFromJsonStr(newMergedWithIdsJson, C.Person.MERGED_WITH_IDS)
    } else {
      List()
    }

    val model = Person(
      id = Some(rec(C.Base.ID).asInstanceOf[String]),
      label = rec(C.Person.LABEL).asInstanceOf[Int],
      name = Some(rec(C.Person.NAME).asInstanceOf[String]),
      mergedWithIds = newMergedWithIdsList,
      mergedIntoId = Some(rec(C.Person.MERGED_INTO_ID).asInstanceOf[String]),
      numOfFaces = rec(C.Person.NUM_OF_FACES).asInstanceOf[Int],
      isHidden = getBooleanField(rec(C.Person.IS_HIDDEN))
    )

    addCoreAttrs(model, rec)
  }

  override def add(jsonIn: JsObject): JsObject = {
    // Get the next person label using the person_label sequence table
    val labelSql = "INSERT INTO person_label DEFAULT VALUES RETURNING id"
    val labelRes = executeAndGetOne(labelSql, List())
    val label = labelRes("id").asInstanceOf[Int]

    val sql =
      s"""
        INSERT INTO $tableName (${C.Person.ID}, ${C.Asset.REPO_ID}, ${C.Person.LABEL}, ${C.Person.NAME})
             VALUES (?, ?, ?, ?)
    """

    val person: Person = jsonIn: Person
    // WARNING: name logic is duplicated for DAOs
    val personName = person.name.getOrElse(s"${Person.UNKNOWN_NAME_PREFIX} $label")
    val id = BaseDao.genId

    val sqlVals: List[Any] = List(
      id,
      RequestContext.getRepository.persistedId,
      label,
      personName
    )

    addRecord(jsonIn, sql, sqlVals)

    jsonIn ++ Json.obj(
      C.Base.ID -> id,
      C.Person.LABEL -> label,
      C.Person.NAME -> Some(personName))
  }

  /**
   * For SQLITE, we keep this list as a JSON array as the DB does not natively support array types
   */
  override def updateMergedWithIds(person: Person, newId: String): Person = {
    val sql =
      s"""
            UPDATE $tableName
               SET ${C.Person.MERGED_WITH_IDS} = ?
             WHERE ${C.Base.ID} = ?
         RETURNING ${C.Person.MERGED_WITH_IDS}
      """

    val updatedIdList = person.mergedWithIds :+ newId

    val mergedWithIdsJson = Json.obj(
      C.Person.MERGED_WITH_IDS -> Json.toJson(updatedIdList),
    )

    val sqlVals: List[Any] = List(
      mergedWithIdsJson.toString(),
      person.persistedId
    )

    val res = executeAndGetOne(sql, sqlVals)
    val newMergedWithIdsJson = res(C.Person.MERGED_WITH_IDS).asInstanceOf[String]
    val newMergedWithIdsList = getListFromJsonStr(newMergedWithIdsJson, C.Person.MERGED_WITH_IDS)
    person.copy(mergedWithIds= newMergedWithIdsList)
  }
}
