package altitude.models

import org.joda.time.format.{ISODateTimeFormat, DateTimeFormatter}
import org.joda.time.DateTime
import play.api.libs.json.{JsValue, Json}
import reactivemongo.bson.BSONObjectID
import altitude.{Const => C}

import scala.language.implicitConversions

object BaseModel {
  final def genId: String = BSONObjectID.generate.stringify
  implicit def toJson(obj: BaseModel): JsValue = obj.toJson
}

abstract class BaseModel(id: String = BaseModel.genId) {

  private val DATETIME_FORMAT: DateTimeFormatter = ISODateTimeFormat.dateTime()

  val createdAt: DateTime = altitude.util.utcNow
  val updatedAt: DateTime = altitude.util.utcNow

  final def isoCreatedAt: String = DATETIME_FORMAT.print(createdAt)
  final def isoUpdatedAt: String = DATETIME_FORMAT.print(updatedAt)

  def toJson: JsValue = Json.obj( //FIXME: should not be implemented here
    C.Base.ID -> id
  )
}
