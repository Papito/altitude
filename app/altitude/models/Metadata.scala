package altitude.models

import play.api.libs.json.{JsNull, JsValue, Json}
import altitude.{Const => C}

import scala.collection.immutable.HashMap
import scala.language.implicitConversions

object Metadata {
  implicit def fromJson(json: JsValue): Metadata = json match {
    case JsNull => new Metadata
    case _ => new Metadata(
        id = (json \ C.Metadata.ID).as[String])
    }
}

case class Metadata(override final val id: String = BaseModel.genId,
                    private val raw: Map[String, String] = new HashMap[String, String]())
  extends BaseModel(id) {
  override def toJson = Json.obj(
    C.Metadata.ID -> id
  )
}