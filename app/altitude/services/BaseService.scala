package altitude.services

import altitude.dao.BaseDao
import altitude.models.BaseModel
import global.Altitude

import scala.concurrent.Future

abstract class BaseService[Model <: BaseModel[ID], ID] {
  protected val DAO: BaseDao[Model]
  protected val app = Altitude.getInstance()

  def add(model: Model): Future[Model] = {
    DAO.add(model)
  }
}
