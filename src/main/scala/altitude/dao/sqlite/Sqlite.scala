package altitude.dao.sqlite

import altitude.models.BaseModel
import altitude.{Const => C}
import org.joda.time.DateTime

trait Sqlite {
  protected def CORE_SQL_VALS_FOR_INSERT = "?"

  protected def DEFAULT_SQL_COLS_FOR_SELECT = s"""
      ${C("Base.ID")}, *,
      CAST(STRFTIME('%s', created_at) AS INT) AS created_at,
      CAST(STRFTIME('%s', updated_at) AS INT) AS updated_at
    """

  protected def CURRENT_TIME_FUNC = "datetime('now', 'localtime')"

  protected def JSON_FUNC = "?"

  protected def addCoreAttrs(model: BaseModel, rec: Map[String, AnyRef]): Unit = {
    val createdAtSeconds = rec.getOrElse(C("Base.CREATED_AT"), 0).asInstanceOf[Int]
    if (createdAtSeconds != 0) {
      model.createdAt = new DateTime(createdAtSeconds.toLong * 1000)
    }

    val updatedAtSeconds = rec.getOrElse(C("Base.UPDATED_AT"), 0).asInstanceOf[Int]
    if (updatedAtSeconds != 0) {
      model.updatedAt = new DateTime(updatedAtSeconds.toLong * 1000)
    }
  }

  protected def GET_DATETIME_FROM_REC(field: String, rec: Map[String, AnyRef]): Option[DateTime] = {
    val seconds = rec.getOrElse(field, 0).asInstanceOf[Int]
    if (seconds != 0) Some(new DateTime(seconds.toLong * 1000)) else None
  }

  protected def DATETIME_TO_DB_FUNC(datetime: Option[DateTime]): String = {
    datetime match {
      case None => null
      case _ => s"strftime('%Y-%m-%d %H:%M:%S Z', ${datetime.get.getMillis} / 1000, 'unixepoch')"
    }
  }

}