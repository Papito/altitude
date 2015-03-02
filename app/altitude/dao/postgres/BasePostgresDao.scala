package altitude.dao.postgres

import play.api.db._

import altitude.dao.BaseDao
import altitude.util.log
import play.api.Play
import play.api.libs.json.{JsValue, Json}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import play.api.Play.current

abstract class BasePostgresDao(private val tableName: String) extends BaseDao {
  private val url = Play.current.configuration.getString("db.postgres.url").getOrElse("")
  require(url.nonEmpty)
  private val driver = Play.current.configuration.getString("db.postgres.driver").getOrElse("")

  def datasource = DB.getDataSource("postgres")
  //def db = Database.forURL()

  override def add(json: JsValue): Future[JsValue] = {
    log.info("POSTGRES INSERT")
    datasource.getConnection
    Future[JsValue] {
      json
    }
  }

  override def getById(id: String): Future[JsValue] = {
    Future[JsValue] {
      Json.obj()
    }
  }
}