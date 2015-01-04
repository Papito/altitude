package service.manager

import dao.common.BaseDao
import global.manager.Altitude
import models.common.BaseModel

import scala.concurrent.Future

abstract class BaseService[Model <: BaseModel[ID], ID] {
  protected val DAO: BaseDao[Model]
  protected val app = Altitude.getInstance()

  def add(model: Model): Future[Model] = {
    DAO.add(model)
  }
}
