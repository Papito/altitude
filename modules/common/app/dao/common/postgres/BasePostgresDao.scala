package dao.common.postgres

import altitude.{Const => C}
import dao.common.BaseDao
import models.common.BaseModel
import play.api.Play
import scala.slick.driver.H2Driver.simple._
import util.log

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

abstract class BasePostgresDao[Model <: BaseModel[ID], ID](private val tableName: String) extends BaseDao[Model] {
  private val url = Play.current.configuration.getString("db.postgres.url").getOrElse("")
  require(url.nonEmpty)
  private val driver = Play.current.configuration.getString("db.postgres.driver").getOrElse("")

  def db = Database.forURL(url, driver = driver)

  override def add(model: Model): Future[Model] = {
    log.info("POSTGRES INSERT")
    Future[Model] {model}
  }
}