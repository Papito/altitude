package software.altitude.core.dao.sqlite

import software.altitude.core.dao.jdbc.BaseDao
import software.altitude.core.models.BaseModel
import software.altitude.core.{Const => C}

import java.text.SimpleDateFormat
import java.time.ZoneId

trait Sqlite { this: BaseDao =>

  override protected def nowTimeFunc = "datetime('now', 'localtime')"

  override protected def jsonFunc = "?"

  override protected def addCoreAttrs(model: BaseModel, rec: Map[String, AnyRef]): model.type = {
    if (rec(C.Base.CREATED_AT) != null) {
      val datetimeAsString = rec(C.Base.CREATED_AT).asInstanceOf[String]
      model.createdAt = stringToLocalDateTime(datetimeAsString)
    }

    if (rec(C.Base.UPDATED_AT) != null) {
      val datetimeAsString = rec(C.Base.UPDATED_AT).asInstanceOf[String]
      model.updatedAt = stringToLocalDateTime(datetimeAsString)
    }

    model
  }

  private def stringToLocalDateTime(datetimeAsString: String): java.time.LocalDateTime = {
    val formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    val date = formatter.parse(datetimeAsString)
    date.toInstant.atZone(ZoneId.systemDefault).toLocalDateTime
  }

}
