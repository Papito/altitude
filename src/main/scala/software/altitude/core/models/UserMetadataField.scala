package software.altitude.core.models

import play.api.libs.json.JsonNaming.SnakeCase
import play.api.libs.json._

import scala.language.implicitConversions

object FieldType extends Enumeration {
  type FieldType = Value
  val KEYWORD: Value = Value("KEYWORD")
  val TEXT: Value = Value("TEXT")
  val NUMBER: Value = Value("NUMBER")
  val BOOL: Value = Value("BOOLEAN")
  val DATETIME: Value = Value("DATETIME")

  implicit val format: Format[FieldType] = Json.formatEnum(this)
}

object UserMetadataField {
  implicit val config: JsonConfiguration = JsonConfiguration(SnakeCase)
  implicit val writes: OWrites[UserMetadataField] = Json.writes[UserMetadataField]
  implicit val reads: Reads[UserMetadataField] = Json.reads[UserMetadataField]

  implicit def fromJson(json: JsValue): UserMetadataField = Json.fromJson[UserMetadataField](json).get
}

case class UserMetadataField(id: Option[String] = None,
                             name: String,
                             fieldType: FieldType.Value) extends BaseModel with NoDates {
  val nameLowercase: String = name.toLowerCase

  val toJson: JsObject = Json.toJson(this).as[JsObject]
}
