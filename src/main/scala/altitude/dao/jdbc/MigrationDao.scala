package altitude.dao.jdbc

import altitude.{Altitude, Context}
import org.apache.commons.dbutils.QueryRunner
import org.slf4j.LoggerFactory

class MigrationDao(app: Altitude) extends altitude.dao.MigrationDao(app) {
  private final val log = LoggerFactory.getLogger(getClass)

  override def currentVersion(implicit ctx: Context): Int = {
    val sql = s"SELECT version FROM $SYSTEM_TABLE"
    val version = try {
      val rec = oneBySqlQuery(sql)
      rec.get("version").asInstanceOf[Int]
    }
    catch {
      // table does not exist
      case ex: java.sql.SQLException => 0
    }
    version
  }

  override def executeCommand(command: String)(implicit ctx: Context): Unit = {
    val stmt = conn.createStatement()
    stmt.executeUpdate(command)
    stmt.close()
  }

  override def versionUp()(implicit ctx: Context): Unit = {
    log.info("VERSION UP")
    val runner: QueryRunner = new QueryRunner()
    val sql = s"UPDATE $SYSTEM_TABLE SET version = 1 WHERE id = 0"
    log.info(sql)
    runner.update(conn, sql)
  }
}
