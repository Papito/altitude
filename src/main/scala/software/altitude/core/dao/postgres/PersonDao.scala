package software.altitude.core.dao.postgres

import com.typesafe.config.Config
import play.api.libs.json.JsObject
import play.api.libs.json.Json
import software.altitude.core.RequestContext
import software.altitude.core.dao.jdbc.BaseDao
import software.altitude.core.models.Person
import software.altitude.core.{Const => C}

class PersonDao(override val config: Config)
  extends software.altitude.core.dao.jdbc.PersonDao(config)
    with PostgresOverrides {

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
}
