package altitude.models

import play.api.libs.json.{JsValue, JsObject, Json}

import scala.language.implicitConversions

object Metadata {
  implicit def toJson(obj: Metadata): JsValue = Json.obj(
    "id" -> obj.id
  )
}

case class Metadata(private val raw: Map[String, String]) extends BaseModel