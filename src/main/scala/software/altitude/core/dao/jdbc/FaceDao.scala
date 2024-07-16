package software.altitude.core.dao.jdbc

import com.typesafe.config.Config
import play.api.libs.json.JsObject
import play.api.libs.json.Json
import software.altitude.core.models.Face
import software.altitude.core.{Const => C}

abstract class FaceDao(override val config: Config) extends BaseDao with software.altitude.core.dao.FaceDao {
  override final val tableName = "face"

  override protected def makeModel(rec: Map[String, AnyRef]): JsObject = {
    val model = Face(
      id = Some(rec(C.Base.ID).asInstanceOf[String]),
    )

    addCoreAttrs(model, rec)
  }

  override def add(jsonIn: JsObject): JsObject = {
    val sql = s"""
        INSERT INTO $tableName (${C.User.ID})
             VALUES (?)
    """

    val face: Face = jsonIn: Face

    val id = BaseDao.genId

    val sqlVals: List[Any] = List(
      id,
    )

    addRecord(jsonIn, sql, sqlVals)
    jsonIn ++ Json.obj(C.Base.ID -> id)
  }
}
