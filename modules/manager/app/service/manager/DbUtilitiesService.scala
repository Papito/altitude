package service.manager

import dao.common.UtilitiesDao
import net.codingwell.scalaguice.InjectorExtensions._

class DbUtilitiesService extends BaseService[Nothing, Nothing] {
  override protected val DAO = app.injector.instance[UtilitiesDao]

  def dropDatabase(): Unit = {
    DAO.dropDatabase()
  }
}
