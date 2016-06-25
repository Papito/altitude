package altitude.dao.postgres

import altitude.models.BaseModel
import altitude.{Const => C}
import org.joda.time.DateTime


trait Postgres {
  protected def CORE_SQL_VALS_FOR_INSERT = "?"

  protected def DEFAULT_SQL_COLS_FOR_SELECT = s"""
      ${C("Base.ID")}, *,
      EXTRACT(EPOCH FROM created_at) AS created_at,
      EXTRACT(EPOCH FROM updated_at) AS updated_at
    """

  protected def CURRENT_TIME_FUNC = "current_timestamp"

  protected def DATETIME_TO_SQL(time: Option[DateTime]): String = {
    time.isEmpty match {
      case true => null
      case false => s"to_timestamp('${time.get}', 'YYYY-MM-DD HH24:MI:SS-XXXX')"
    }
  }

  protected def JSON_PLACEHOLDER = "CAST(? as jsonb)"

  protected def addCoreAttrs(model: BaseModel, rec: Map[String, AnyRef]): Unit = {
    val createdAtMilis = rec.getOrElse(C("Base.CREATED_AT"), 0d).asInstanceOf[Double].toLong
    if (createdAtMilis != 0d) {
      model.createdAt = new DateTime(createdAtMilis * 1000)
    }

    val updatedAtMilis = rec.getOrElse(C("Base.UPDATED_AT"), 0d).asInstanceOf[Double].toLong
    if (updatedAtMilis != 0d) {
      model.updatedAt = new DateTime(createdAtMilis * 1000)
    }
  }
}
