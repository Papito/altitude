package dao.postgres

import altitude.{Const => C}
import dao.BaseDao
import models.BaseModel
import play.api.Play
import play.api.Play.current
import play.api.db._
import util.log

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

abstract class BasePostgresDao[Model <: BaseModel[ID], ID](private val tableName: String) extends BaseDao[Model] {
  private val url = Play.current.configuration.getString("db.postgres.url").getOrElse("")
  require(url.nonEmpty)

  private def conn: java.sql.Connection = {
    log.info("Getting postgres connection @ $host", Map("host" -> url), C.tag.DB, C.tag.APP)
    DB.getConnection("postgres")
  }

  override def add(model: Model): Future[Model] = {
    log.info("POSTGRES INSERT")
    Future[Model] {model}
  }
}