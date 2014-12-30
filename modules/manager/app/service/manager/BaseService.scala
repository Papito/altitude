package service.manager

import dao.mongo.BaseDao
import models.BaseModel
import reactivemongo.core.commands.LastError

import scala.concurrent.Future

abstract class BaseService[T <: BaseModel] {
  protected val DAO: BaseDao[T]
  protected val app = global.ManagerGlobal

  def add(model: T): Future[LastError] = {
    DAO.add(model)
  }

}
