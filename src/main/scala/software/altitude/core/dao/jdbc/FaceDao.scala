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
      x1 = rec(C.Face.X1).asInstanceOf[Int],
      y1 = rec(C.Face.Y1).asInstanceOf[Int],
      x2 = rec(C.Face.X2).asInstanceOf[Int],
      y2 = rec(C.Face.Y2).asInstanceOf[Int]
    )

    addCoreAttrs(model, rec)
  }

  override def add(jsonIn: JsObject): JsObject = {
    val sql = s"""
        INSERT INTO $tableName (${C.Face.ID}, ${C.Face.X1}, ${C.Face.Y1}, ${C.Face.X2}, ${C.Face.Y2})
             VALUES (?, ?, ?, ?, ?)
    """

    val face: Face = jsonIn: Face

    val id = BaseDao.genId

    val sqlVals: List[Any] = List(
      id,
      face.x1,
      face.y1,
      face.x2,
      face.y2
    )

    addRecord(jsonIn, sql, sqlVals)
    jsonIn ++ Json.obj(C.Base.ID -> id)
  }
}
