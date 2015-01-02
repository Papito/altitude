package service.manager

import dao.mongo.BaseDao
import models.BaseModel
import reactivemongo.core.commands.LastError

import scala.concurrent.Future

abstract class BaseService[Model <: BaseModel[ID], ID] {
  protected def DAO: BaseDao[Model, ID]
  protected val app = global.ManagerGlobal

  def add(model: Model): Future[LastError] = {
    DAO.add(model)
  }

}
