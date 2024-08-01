package software.altitude.core.dao.postgres

import com.typesafe.config.Config
import org.postgresql.jdbc.PgArray
import play.api.libs.json.{JsObject, Json}
import software.altitude.core.dao.jdbc.BaseDao
import software.altitude.core.models.Person
import software.altitude.core.{RequestContext, Const => C}

class PersonDao(override val config: Config)
  extends BaseDao
    with software.altitude.core.dao.PersonDao
    with PostgresOverrides {

  override final val tableName = "person"

  override protected def makeModel(rec: Map[String, AnyRef]): JsObject = {
    val mergedWithIds = rec(C.Person.MERGED_WITH_IDS).asInstanceOf[PgArray].getArray.asInstanceOf[Array[String]]
    println("!!!")
    println(mergedWithIds.mkString("Array(", ", ", ")"))
    val model = Person(
      id = Some(rec(C.Base.ID).asInstanceOf[String]),
      label = rec(C.Person.LABEL).asInstanceOf[Long],
      name = rec(C.Person.NAME).asInstanceOf[String],
      mergedWithIds = List(),
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
    val label = labelRes("nextval").asInstanceOf[Long]

    val sql =
      s"""
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
