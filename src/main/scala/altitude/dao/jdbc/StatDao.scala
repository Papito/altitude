package altitude.dao.jdbc

import altitude.transactions.TransactionId
import altitude.Altitude
import altitude.models.Stat
import altitude.{Const => C}
import org.apache.commons.dbutils.QueryRunner
import org.slf4j.LoggerFactory
import play.api.libs.json.JsObject

abstract class StatDao (val app: Altitude) extends BaseJdbcDao("stats") with altitude.dao.StatDao {
  private final val log = LoggerFactory.getLogger(getClass)

  override protected def makeModel(rec: Map[String, AnyRef]): JsObject = Stat(
    rec.get("dimension").get.asInstanceOf[String], rec.get("dim_val").get.asInstanceOf[Int])

  def incrementStat(statName: String, count: Int = 1)(implicit txId: TransactionId): Unit = {
    val sql = s"""
      UPDATE $tableName
         SET ${C("Stat.DIM_VAL")} = ${C("Stat.DIM_VAL")} + $count
       WHERE ${C("Stat.DIMENSION")} = ?
      """
    log.debug(s"INCR STAT SQL: $sql, for $statName")

    val runner: QueryRunner = new QueryRunner()
    runner.update(conn, sql, statName)
  }
}
