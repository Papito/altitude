package altitude.service

import altitude.Altitude
import altitude.dao.NotImplementedDao

class SearchService(val app: Altitude) extends BaseService {
  override protected val DAO = new NotImplementedDao(app)
}
