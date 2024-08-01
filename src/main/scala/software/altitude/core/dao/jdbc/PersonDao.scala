package software.altitude.core.dao.jdbc

import com.typesafe.config.Config
import play.api.libs.json.JsObject
import play.api.libs.json.Json
import software.altitude.core.models.Person
import software.altitude.core.{RequestContext, Const => C}

abstract class PersonDao(override val config: Config) extends BaseDao with software.altitude.core.dao.PersonDao {
  override final val tableName = "person"

  override protected def makeModel(rec: Map[String, AnyRef]): JsObject = {
    val mergedWithIdsJson = rec(C.Person.MERGED_WITH_IDS).asInstanceOf[String]
    println("!!!")
    println(mergedWithIdsJson)
    val model = Person(
      id = Some(rec(C.Base.ID).asInstanceOf[String]),
      label = rec(C.Person.LABEL).asInstanceOf[Int],
      name = rec(C.Person.NAME).asInstanceOf[String],
      mergedWithIds = List(),
      mergedIntoId = Some(rec(C.Person.MERGED_INTO_ID).asInstanceOf[String]),
      numOfFaces = rec(C.Person.NUM_OF_FACES).asInstanceOf[Int]
    )

    addCoreAttrs(model, rec)
  }

  override def add(jsonIn: JsObject): JsObject = {
    /**
     * Get the next person label using the person_label sequence table
     */
    val labelSql = "INSERT INTO person_label DEFAULT VALUES RETURNING id"
    val labelRes = executeAndGetOne(labelSql, List())
    val label = labelRes("id").asInstanceOf[Int]

    val sql = s"""
        INSERT INTO $tableName (${C.Person.ID}, ${C.Asset.REPO_ID}, ${C.Person.LABEL}, ${C.Person.NAME})
              VALUES (?, ?, ?, ?)
    """

    val person: Person = jsonIn: Person
    val id = BaseDao.genId

    val sqlVals: List[Any] = List(
      id,
      RequestContext.getRepository.persistedId,
      label,
      person.name,
    )

    addRecord(jsonIn, sql, sqlVals)

    jsonIn ++ Json.obj(
      C.Base.ID -> id,
      C.Person.LABEL -> label)
  }
}
