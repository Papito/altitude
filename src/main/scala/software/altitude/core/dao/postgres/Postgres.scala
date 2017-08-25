package software.altitude.core.dao.postgres

import java.sql.Timestamp

import org.joda.time.DateTime
import software.altitude.core.dao.jdbc.BaseJdbcDao
import software.altitude.core.models.BaseModel
import software.altitude.core.{Const => C}


trait Postgres { this: BaseJdbcDao => // can only be mixed into the subclasses of this

  override protected def DEFAULT_SQL_COLS_FOR_SELECT = s"""
      ${C.Base.ID}, *,
      EXTRACT(EPOCH FROM created_at) AS created_at,
      EXTRACT(EPOCH FROM updated_at) AS updated_at
    """

  override protected def CURRENT_TIME_FUNC = "current_timestamp"

  override protected def JSON_FUNC = "CAST(? as jsonb)"

  override protected def addCoreAttrs(model: BaseModel, rec: Map[String, AnyRef]): model.type = {
    val createdAtMilis = rec.getOrElse(C.Base.CREATED_AT, 0d).asInstanceOf[Double].toLong
    if (createdAtMilis != 0d) {
      model.createdAt = new DateTime(createdAtMilis * 1000)
    }

    val updatedAtMilis = rec.getOrElse(C.Base.UPDATED_AT, 0d).asInstanceOf[Double].toLong
    if (updatedAtMilis != 0d) {
      model.updatedAt = new DateTime(createdAtMilis * 1000)
    }

    model
  }

  override protected def GET_DATETIME_FROM_REC(field: String, rec: Map[String, AnyRef]): Option[DateTime] = {
    val timestamp: Timestamp = rec.get(field).get.asInstanceOf[Timestamp]
    val dt = new DateTime(timestamp.getTime).withMillisOfSecond(0)
    Some(dt)
  }

  override protected def DATETIME_TO_DB_FUNC(datetime: Option[DateTime]): String = {
    datetime match {
      case None => null
      case _ => s"to_timestamp(${datetime.get.getMillis} / 1000)"
    }
  }

}

