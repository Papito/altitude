package software.altitude.core.dao.postgres
import software.altitude.core.dao.jdbc.BaseDao
import software.altitude.core.models.BaseModel
import software.altitude.core.{Const => C}


trait Postgres { this: BaseDao =>
  override protected def nowTimeFunc = "current_timestamp"

  override protected def jsonFunc = "CAST(? as jsonb)"

  override protected def addCoreAttrs(model: BaseModel, rec: Map[String, AnyRef]): model.type = {
    if (rec(C.Base.CREATED_AT) != null) {
      val createdAtTimestamp = rec(C.Base.CREATED_AT).asInstanceOf[java.sql.Timestamp]
      model.createdAt = createdAtTimestamp.toLocalDateTime
    }

    if (rec(C.Base.UPDATED_AT) != null) {
      val updatedAtTimestamp = rec(C.Base.UPDATED_AT).asInstanceOf[java.sql.Timestamp]
      model.updatedAt = updatedAtTimestamp.toLocalDateTime
    }

    model
  }
}
