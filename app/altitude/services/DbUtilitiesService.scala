package altitude.services

import altitude.dao.UtilitiesDao
import net.codingwell.scalaguice.InjectorExtensions._

class DbUtilitiesService extends BaseService {
  override protected val DAO = app.injector.instance[UtilitiesDao]

  def dropDatabase(): Unit = {
    DAO.dropDatabase()
  }
}
