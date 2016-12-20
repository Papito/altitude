package altitude.dao.jdbc

import altitude.models.Stat
import altitude.{Altitude, Const => C, Context}
import org.apache.commons.dbutils.QueryRunner
import org.slf4j.LoggerFactory
import play.api.libs.json.JsObject

abstract class StatDao (val app: Altitude) extends BaseJdbcDao("stats") with altitude.dao.StatDao {
  private final val log = LoggerFactory.getLogger(getClass)

  override protected def makeModel(rec: Map[String, AnyRef]): JsObject = Stat(
    rec.get("user_id").get.asInstanceOf[String],
    rec.get("dimension").get.asInstanceOf[String],
    rec.get("dim_val").get.asInstanceOf[Int])

  override def add(jsonIn: JsObject)(implicit ctx: Context): JsObject = {
    val sql: String =s"""
      INSERT INTO $tableName (user_id, dimension)
           VALUES (? ,?)"""

    val stat: Stat = jsonIn
    val values: List[Object] = ctx.user.get.id.get :: stat.dimension :: Nil

    addRecord(jsonIn, sql, values)
  }

  override protected def addRecord(jsonIn: JsObject, q: String, vals: List[Object])
                         (implicit ctx: Context): JsObject = {
    log.info(s"JDBC INSERT: $jsonIn")
    val runner: QueryRunner = new QueryRunner()
    runner.update(conn, q, vals:_*)
    jsonIn
  }

  def incrementStat(statName: String, count: Long = 1)
                   (implicit ctx: Context): Unit = {
    val sql = s"""
      UPDATE $tableName
         SET ${C.Stat.DIM_VAL} = ${C.Stat.DIM_VAL} + $count
       WHERE ${C.Base.USER_ID} = ? and ${C.Stat.DIMENSION} = ?
      """
    log.debug(s"INCR STAT SQL: $sql, for $statName")

    val runner: QueryRunner = new QueryRunner()
    runner.update(conn, sql, ctx.user.get.id.get, statName)
  }
}
