package service.manager

import dao.BaseDao
import models.BaseModel
import scala.concurrent.Future
import global.ManagerGlobal
import scala.concurrent.ExecutionContext.Implicits.global

abstract class BaseService[Model <: BaseModel[ID], ID] {
  protected val DAO: BaseDao[Model]
  protected val app = ManagerGlobal

  def add(model: Model): Future[Model] = {
    DAO.add(model)
    Future[Model] {model}
  }
}
