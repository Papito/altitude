package altitude.dao.postgres

import altitude.dao.BaseDao
import altitude.models.BaseModel
import altitude.util.log
import play.api.Play

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.slick.driver.H2Driver.simple._

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