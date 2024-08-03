package software.altitude.core.dao.jdbc

import com.typesafe.config.Config
import play.api.libs.json.{JsObject}
import software.altitude.core.models.Person
import software.altitude.core.{Const => C}

abstract class PersonDao(override val config: Config) extends BaseDao with software.altitude.core.dao.PersonDao {

  override final val tableName = "person"

  override protected def makeModel(rec: Map[String, AnyRef]): JsObject = {

    val model = Person(
      id = Some(rec(C.Base.ID).asInstanceOf[String]),
      // To placate Postgres Sequences, which return Longs
      label = rec(C.Person.LABEL).getClass match {
        case c if c == classOf[java.lang.Integer] => rec(C.Person.LABEL).asInstanceOf[Int]
        case c if c == classOf[java.lang.Long] => rec(C.Person.LABEL).asInstanceOf[Long].toInt
      },
      name = Some(rec(C.Person.NAME).asInstanceOf[String]),
      coverFaceId = Some(rec(C.Person.COVER_FACE_ID).asInstanceOf[String]),
      mergedWithIds = loadCsv[String](rec(C.Person.MERGED_WITH_IDS).asInstanceOf[String]),
      mergedIntoId = Some(rec(C.Person.MERGED_INTO_ID).asInstanceOf[String]),
      numOfFaces = rec(C.Person.NUM_OF_FACES).asInstanceOf[Int],
      isHidden = getBooleanField(rec(C.Person.IS_HIDDEN))
    )

    addCoreAttrs(model, rec)
  }

  override def updateMergedWithIds(person: Person, newId: String): Person = {
    val updatedIdList = person.mergedWithIds :+ newId

    val mergedWithIdsCsv = makeCsv(updatedIdList)

    val sqlVals: List[Any] = List(
      mergedWithIdsCsv,
      person.persistedId
    )

    val sql =
      s"""
            UPDATE $tableName
               SET ${C.Person.MERGED_WITH_IDS} = ?
             WHERE ${C.Base.ID} = ?
      """

    updateByBySql(sql, sqlVals)
    person.copy(mergedWithIds = updatedIdList)
  }
}
