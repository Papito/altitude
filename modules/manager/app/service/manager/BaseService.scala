package service.manager

import dao.common.BaseDao
import global.Altitude
import models.BaseModel

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

abstract class BaseService[Model <: BaseModel[ID], ID] {
  protected val DAO: BaseDao[Model]
  protected val app = Altitude.getInstance()

  def add(model: Model): Future[Model] = {
    DAO.add(model)
  }
}
