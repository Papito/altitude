package software.altitude.core.dao.sqlite

import com.typesafe.config.Config
import play.api.libs.json.JsObject
import play.api.libs.json.Json
import software.altitude.core.FieldConst
import software.altitude.core.RequestContext
import software.altitude.core.dao.jdbc.BaseDao
import software.altitude.core.models.Person
import software.altitude.core.service.FaceRecognitionService

class PersonDao(override val config: Config) extends software.altitude.core.dao.jdbc.PersonDao(config) with SqliteOverrides {

  override def add(jsonIn: JsObject): JsObject = {
    // Get the next person label using the person_label sequence table
    val labelSql = "INSERT INTO person_label DEFAULT VALUES RETURNING id"
    val labelRes = executeAndGetOne(labelSql, List())
    val label = labelRes("id").asInstanceOf[Int]
    val personSeqNum = label - FaceRecognitionService.RESERVED_LABEL_COUNT

    val sql =
      s"""
        INSERT INTO $tableName (${FieldConst.ID},
                                ${FieldConst.REPO_ID},
                                ${FieldConst.Person.LABEL},
                                ${FieldConst.Person.NAME},
                                ${FieldConst.Person.NAME_FOR_SORT})
             VALUES (?, ?, ?, ?, ?)
    """

    val person: Person = jsonIn: Person
    val personName = getPersonName(person, personSeqNum)
    val personSortName = getPersonSortName(person, personSeqNum)
    val id = BaseDao.genId

    val sqlVals: List[Any] = List(
      id,
      RequestContext.getRepository.persistedId,
      label,
      personName,
      personSortName
    )

    addRecord(jsonIn, sql, sqlVals)

    jsonIn ++ Json.obj(FieldConst.ID -> id, FieldConst.Person.LABEL -> label, FieldConst.Person.NAME -> Some(personName))
  }
}
