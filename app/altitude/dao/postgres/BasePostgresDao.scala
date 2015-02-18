package altitude.dao.postgres

import altitude.dao.BaseDao
import altitude.util.log
import play.api.Play
import play.api.libs.json.{JsValue, Json}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.slick.driver.H2Driver.simple._

abstract class BasePostgresDao(private val tableName: String) extends BaseDao {
  private val url = Play.current.configuration.getString("db.postgres.url").getOrElse("")
  require(url.nonEmpty)
  private val driver = Play.current.configuration.getString("db.postgres.driver").getOrElse("")

  def db = Database.forURL(url, driver = driver)

  override def add(json: JsValue): Future[JsValue] = {
    log.info("POSTGRES INSERT")
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