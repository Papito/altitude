package altitude.manager.services

import altitude.common.dao.BaseDao
import altitude.common.models.BaseModel
import global.manager.Altitude

import scala.concurrent.Future

abstract class BaseService[Model <: BaseModel[ID], ID] {
  protected val DAO: BaseDao[Model]
  protected val app = Altitude.getInstance()

  def add(model: Model): Future[Model] = {
    DAO.add(model)
  }
}
