package dao.common.postgres

import altitude.{Const => C}
import dao.common.BaseDao
import models.common.BaseModel
import play.api.Play
import play.api.Play.current
import play.api.db._
import util.log

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

abstract class BasePostgresDao[Model <: BaseModel[ID], ID](private val tableName: String) extends BaseDao[Model] {
  private val url = Play.current.configuration.getString("db.postgres.url").getOrElse("")
  require(url.nonEmpty)

  protected lazy val ds: javax.sql.DataSource = DB.getDataSource("postgres")
  private def conn: java.sql.Connection = ds.getConnection

  override def add(model: Model): Future[Model] = {
    log.info("POSTGRES INSERT")
    Future[Model] {model}
  }
}