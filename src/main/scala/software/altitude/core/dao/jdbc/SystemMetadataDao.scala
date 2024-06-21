package software.altitude.core.dao.jdbc

import play.api.libs.json.JsObject
import software.altitude.core.AltitudeAppContext
import software.altitude.core.models.SystemMetadata
import software.altitude.core.{Const => C}

import java.sql.SQLException

object SystemMetadataDao {
  // we only have one record in the system table at all times
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
    txManager.asReadOnly[SystemMetadata] {
      getById(SystemMetadataDao.SYSTEM_RECORD_ID.toString).get
    }
  }

  override def version: Int = {
    try {
      read.version
    }
    catch {
      case _: SQLException => 0 // new installation
      case ex: Exception => throw ex
    }
  }

  // overriding the base method since there is no repository relation in this model
  override def getById(id: String): Option[JsObject] = {
    val sql: String = s"""
      SELECT *
        FROM $tableName
       WHERE id = ?
     """

    val rec = oneBySqlQuery(sql, List(id.toInt))
    if (rec.isDefined) Some(makeModel(rec.get)) else None
  }

}
