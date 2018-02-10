package software.altitude.core.models

import play.api.libs.json.{JsString, JsNull, JsValue, Json}
import software.altitude.core.{Const => C}
import scala.language.implicitConversions

object MetadataValue {
  implicit def fromJson(json: JsValue): MetadataValue = {
    MetadataValue(
      id = (json \ C.Base.ID).asOpt[String],
      value = (json \ C.Base.VALUE).as[String]
    )
  }

  def apply(value: String): MetadataValue = MetadataValue(id = None, value = value)
}

case class MetadataValue(id: Option[String] = None,
                         value: String) extends BaseModel {
  private val md = java.security.MessageDigest.getInstance("SHA-1")
  val checksum = new sun.misc.BASE64Encoder().encode(md.digest(value.toLowerCase.getBytes("UTF-8")))

  override def canEqual(other: Any): Boolean = other.isInstanceOf[MetadataValue]

  final def nonEmpty = value.nonEmpty

  override def equals( that: Any): Boolean = that match {
    case that: MetadataValue if !that.canEqual( this) => false
    case that: MetadataValue => this.checksum == that.checksum
    case _ => false
  }

  override def toJson = Json.obj(
    C.Base.VALUE -> value,
    C.Base.ID -> {id match {
      case None => JsNull
      case _ => JsString(id.get)
    }}
  )

}
