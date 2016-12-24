package altitude.dao.postgres

import java.sql.Timestamp

import altitude.models.BaseModel
import altitude.{Const => C}
import org.joda.time.DateTime


trait Postgres {
  protected def CORE_SQL_VALS_FOR_INSERT = "?, ?"

  protected def DEFAULT_SQL_COLS_FOR_SELECT = s"""
      ${C.Base.ID}, *,
      EXTRACT(EPOCH FROM created_at) AS created_at,
      EXTRACT(EPOCH FROM updated_at) AS updated_at
    """

  protected def CURRENT_TIME_FUNC = "current_timestamp"

  protected def JSON_FUNC = "CAST(? as jsonb)"

  protected def addCoreAttrs(model: BaseModel, rec: Map[String, AnyRef]): model.type = {
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

  protected def GET_DATETIME_FROM_REC(field: String, rec: Map[String, AnyRef]): Option[DateTime] = {
    val timestamp: Timestamp = rec.get(field).get.asInstanceOf[Timestamp]
    val dt = new DateTime(timestamp.getTime).withMillisOfSecond(0)
    Some(dt)
  }

  protected def DATETIME_TO_DB_FUNC(datetime: Option[DateTime]): String = {
    datetime match {
      case None => null
      case _ => s"to_timestamp(${datetime.get.getMillis} / 1000)"
    }
  }

}

