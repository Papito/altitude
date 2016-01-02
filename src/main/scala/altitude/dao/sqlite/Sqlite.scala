package altitude.dao.sqlite

import altitude.models.BaseModel
import altitude.{Const => C}
import org.joda.time.DateTime

trait Sqlite {
  protected def CORE_SQL_VALS_FOR_INSERT = "?"

  protected def DEFAULT_SQL_COLS_FOR_SELECT = s"""
      ${C.Base.ID}, *,
      CAST(STRFTIME('%s', created_at) AS INT) AS created_at,
      CAST(STRFTIME('%s', updated_at) AS INT) AS updated_at
    """

  protected def CURRENT_TIME_FUNC = "datetime('now', 'localtime')"

  protected def JSON_PLACEHOLDER = "?"

  protected def addCoreAttrs(model: BaseModel, rec: Map[String, AnyRef]): Unit = {
    val createdAtSeconds = rec.getOrElse(C.Base.CREATED_AT, 0).asInstanceOf[Int]
    if (createdAtSeconds != 0) {
      model.createdAt = new DateTime(createdAtSeconds.toLong * 1000)
    }

    val updatedAtSeconds = rec.getOrElse(C.Base.UPDATED_AT, 0).asInstanceOf[Int]
    if (updatedAtSeconds != 0) {
      model.updatedAt = new DateTime(updatedAtSeconds.toLong * 1000)
    }
  }
}