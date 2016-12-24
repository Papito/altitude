package altitude.dao.jdbc

import altitude.models.Stat
import altitude.transactions.TransactionId
import altitude.{Altitude, Const => C, Context}
import org.apache.commons.dbutils.QueryRunner
import org.slf4j.LoggerFactory
import play.api.libs.json.JsObject

abstract class StatDao (val app: Altitude) extends BaseJdbcDao("stats") with altitude.dao.StatDao {
  private final val log = LoggerFactory.getLogger(getClass)

  override protected def makeModel(rec: Map[String, AnyRef]): JsObject = Stat(
    rec.get(C.Base.REPO_ID).get.asInstanceOf[String],
    rec.get(C.Stat.DIMENSION).get.asInstanceOf[String],
    rec.get(C.Stat.DIM_VAL).get.asInstanceOf[Int])

  override def add(jsonIn: JsObject)(implicit ctx: Context, txId: TransactionId): JsObject = {
    val sql: String =s"""
      INSERT INTO $tableName (${C.Base.REPO_ID}, ${C.Stat.DIMENSION})
           VALUES (? ,?)"""

    val stat: Stat = jsonIn
    val values: List[Object] = ctx.repo.id.get :: stat.dimension :: Nil

    addRecord(jsonIn, sql, values)
  }

  override protected def addRecord(jsonIn: JsObject, q: String, vals: List[Object])
                         (implicit ctx: Context, txId: TransactionId): JsObject = {
    log.info(s"JDBC INSERT: $jsonIn")
    val runner: QueryRunner = new QueryRunner()
    runner.update(conn, q, vals:_*)
    jsonIn
  }

  def incrementStat(statName: String, count: Long = 1)
                   (implicit ctx: Context, txId: TransactionId): Unit = {
    val sql = s"""
      UPDATE $tableName
         SET ${C.Stat.DIM_VAL} = ${C.Stat.DIM_VAL} + $count
       WHERE ${C.Base.REPO_ID} = ? and ${C.Stat.DIMENSION} = ?
      """
    log.debug(s"INCR STAT SQL: $sql, for $statName")

    val runner: QueryRunner = new QueryRunner()
    runner.update(conn, sql, ctx.repo.id.get, statName)
  }
}
