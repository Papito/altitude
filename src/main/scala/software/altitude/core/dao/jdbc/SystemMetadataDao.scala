package software.altitude.core.dao.jdbc

import play.api.libs.json.JsObject
import software.altitude.core.AltitudeAppContext
import software.altitude.core.models.SystemMetadata
import software.altitude.core.{Const => C}

object SystemMetadataDao {
  private val SYSTEM_RECORD_ID = 1
}

abstract class SystemMetadataDao(val appContext: AltitudeAppContext)
  extends BaseDao with software.altitude.core.dao.SystemMetadataDao {

  override val tableName = "system"

  override protected def makeModel(rec: Map[String, AnyRef]): JsObject = SystemMetadata(
    rec(C.SystemMetadata.VERSION).asInstanceOf[Int],
    rec(C.SystemMetadata.IS_INITIALIZED).asInstanceOf[Boolean]
  ).toJson

  override def read: SystemMetadata = {
    getById(SystemMetadataDao.SYSTEM_RECORD_ID.toString).get
  }
}
