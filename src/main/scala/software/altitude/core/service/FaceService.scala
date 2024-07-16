package software.altitude.core.service

import software.altitude.core.Altitude
import software.altitude.core.dao.FaceDao
import software.altitude.core.models.Face

class FaceService(val app: Altitude) extends BaseService[Face] {

  override protected val dao: FaceDao = app.DAO.face

}
