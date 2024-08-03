package software.altitude.core.dao.postgres

import com.typesafe.config.Config
import org.postgresql.jdbc.PgArray
import play.api.libs.json.JsObject
import play.api.libs.json.Json
import software.altitude.core.RequestContext
import software.altitude.core.dao.jdbc.BaseDao
import software.altitude.core.models.Person
import software.altitude.core.{Const => C}

import java.sql.PreparedStatement

class PersonDao(override val config: Config)
  extends BaseDao
    with software.altitude.core.dao.PersonDao
    with PostgresOverrides {

  override final val tableName = "person"

  override protected def makeModel(rec: Map[String, AnyRef]): JsObject = {
    // get a list persons this record has been merged with
    val mergedWithIdsRecVal = rec(C.Person.MERGED_WITH_IDS)
    val mergedWIthIdsList = if (mergedWithIdsRecVal != null) {
      mergedWithIdsRecVal.asInstanceOf[PgArray].getArray.asInstanceOf[Array[String]].toList
    } else {
        List()
    }

    val model = Person(
      id = Some(rec(C.Base.ID).asInstanceOf[String]),
      label = rec(C.Person.LABEL).asInstanceOf[Long],
      coverFaceId = Some(rec(C.Person.COVER_FACE_ID).asInstanceOf[String]),
      name = Some(rec(C.Person.NAME).asInstanceOf[String]),
      mergedWithIds = mergedWIthIdsList,
      mergedIntoId = Some(rec(C.Person.MERGED_INTO_ID).asInstanceOf[String]),
      numOfFaces = rec(C.Person.NUM_OF_FACES).asInstanceOf[Int],
      isHidden = getBooleanField(rec(C.Person.IS_HIDDEN))
    )

    addCoreAttrs(model, rec)
  }

  override def add(jsonIn: JsObject): JsObject = {
    /**
     * Get the next person label using the person_label sequence table
     */
    val labelSql = "SELECT nextval('person_label')"
    val labelRes = executeAndGetOne(labelSql, List())
    val label = labelRes("nextval").asInstanceOf[Long] - Person.RESERVED_LABEL_COUNT

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
      personName,
    )

    addRecord(jsonIn, sql, sqlVals)

    jsonIn ++ Json.obj(
      C.Base.ID -> id,
      C.Person.LABEL -> label,
      C.Person.NAME -> Some(personName))
  }

  override def updateMergedWithIds(person: Person, newId: String): Person = {
    val updatedIdList = person.mergedWithIds :+ newId

    val sql =
      s"""
        UPDATE $tableName
           SET ${C.Person.MERGED_WITH_IDS} = ?
         WHERE ${C.Base.ID} = ?
      """
    val conn = RequestContext.getConn

    val updatedIdListAsSqlArray = conn.createArrayOf("text", updatedIdList.toArray)

    val preparedStatement: PreparedStatement = conn.prepareStatement(sql)
    preparedStatement.setObject(1, updatedIdListAsSqlArray)
    preparedStatement.setString(2, person.persistedId)
    preparedStatement.execute()

    person.copy(mergedWithIds = updatedIdList)
  }
}
