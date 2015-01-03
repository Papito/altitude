package service.manager

import dao.BaseDao
import global.ManagerGlobal
import models.BaseModel

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

abstract class BaseService[Model <: BaseModel[ID], ID] {
  protected val DAO: BaseDao[Model]
  protected val app = ManagerGlobal

  def add(model: Model): Future[Model] = {
    DAO.add(model)
    Future[Model] {model}
  }
}
