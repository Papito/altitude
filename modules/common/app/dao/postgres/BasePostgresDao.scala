package dao.postgres

import altitude.{Const => C}
import dao.BaseDao
import models.BaseModel
import util.log
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.Future
object BasePostgresDao {
  log.info(
    "Initializing postgres connection",
    C.tag.DB, C.tag.APP)
}

abstract class BasePostgresDao[Model <: BaseModel[ID], ID](private val tableName: String) extends BaseDao[Model] {

  override def add(model: Model): Future[Model] = {
    log.info("!!!ADD")
    Future[Model] {model}
  }
}