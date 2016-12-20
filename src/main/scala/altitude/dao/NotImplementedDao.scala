package altitude.dao

import altitude.models.search.{Query, QueryResult}
import altitude.{Altitude, Context}
import play.api.libs.json.JsObject

class NotImplementedDao(val app: Altitude) extends BaseDao {
  def add(json: JsObject)(implicit ctx: Context): JsObject =
    throw new NotImplementedError("NOT IMPLEMENTED DAO CALLED - YOU ARE USING WRONG SERVICE CLASS METHOD!")
  def deleteByQuery(q: Query)(implicit ctx: Context): Int =
    throw new NotImplementedError("NOT IMPLEMENTED DAO CALLED - YOU ARE USING WRONG SERVICE CLASS METHOD!")
  def getById(id: String)(implicit ctx: Context): Option[JsObject] =
    throw new NotImplementedError("NOT IMPLEMENTED DAO CALLED - YOU ARE USING BASE SERVICE CLASS METHOD!")
  def getByIds(id: Set[String])(implicit ctx: Context): List[JsObject] =
    throw new NotImplementedError("NOT IMPLEMENTED DAO CALLED - YOU ARE USING WRONG SERVICE CLASS METHOD!")
  def query(q: Query)(implicit ctx: Context): QueryResult =
    throw new NotImplementedError("NOT IMPLEMENTED DAO CALLED - YOU ARE USING WRONG SERVICE CLASS METHOD!")
  def updateByQuery(q: Query, data: JsObject, fields: List[String])(implicit ctx: Context): Int =
    throw new NotImplementedError("NOT IMPLEMENTED DAO CALLED - YOU ARE USING WRONG SERVICE CLASS METHOD!")
  def increment(id: String, field: String, count: Int = 1)(implicit ctx: Context) =
    throw new NotImplementedError("NOT IMPLEMENTED DAO CALLED - YOU ARE USING WRONG SERVICE CLASS METHOD!")
  def decrement(id: String, field: String, count: Int = 1)(implicit ctx: Context) =
    throw new NotImplementedError("NOT IMPLEMENTED DAO CALLED - YOU ARE USING WRONG SERVICE CLASS METHOD!")
}
