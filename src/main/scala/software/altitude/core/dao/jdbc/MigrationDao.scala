package software.altitude.core.dao.jdbc

import org.apache.commons.dbutils.QueryRunner
import org.slf4j.LoggerFactory
import play.api.libs.json.JsObject
import software.altitude.core.AltitudeAppContext

abstract class MigrationDao(val appContext: AltitudeAppContext)
  extends BaseDao with software.altitude.core.dao.MigrationDao {

  private final val log = LoggerFactory.getLogger(getClass)

  override val tableName = "system"

  /**
   * Get current version of the schema
   *
   * @return The integer version
   */
  override def currentVersion: Int = {
    val sql = s"SELECT version FROM $tableName"
    val version = try {
      val rec = oneBySqlQuery(sql)
      rec.get("version").asInstanceOf[Int]
    }
    catch {
      // table does not exist
      case _: java.sql.SQLException => 0
    }
    version
  }

  /**
   * Execute an arbitrary command
   */
  override def executeCommand(command: String): Unit = {
    val stmt = conn.createStatement()
    stmt.executeUpdate(command)
    stmt.close()
  }

  /**
   * Up the schema version by one after completion
   */
  override def versionUp(): Unit = {
    log.info("VERSION UP")
    val runner: QueryRunner = new QueryRunner()
    val sql = s"UPDATE $tableName SET version = 1 WHERE id = 0"
    log.info(sql)
    runner.update(conn, sql)
  }

  protected override  def makeModel(rec: Map[String, AnyRef]): JsObject = throw new NotImplementedError
}
