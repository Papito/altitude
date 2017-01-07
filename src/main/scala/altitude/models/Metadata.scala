package altitude.models

import play.api.libs.json._

import scala.language.implicitConversions

object Metadata {
  implicit def fromJson(json: JsObject): Metadata = {
    val data = json.keys.foldLeft(Map[String, Set[String]]()) { (res, fieldId) =>
      res + (fieldId -> (json \ fieldId).as[Set[String]])
    }

    new Metadata(data)
  }
}

class Metadata(val data: Map[String, Set[String]] = Map[String, Set[String]]())
  extends BaseModel with NoId {

  override val toJson = data.foldLeft(Json.obj()) { (res, m) =>
    val fieldName = m._1

    val valuesJsArray: JsArray = JsArray(m._2.toSeq.map(JsString))

    // append to the resulting JSON object
    res ++ Json.obj(fieldName -> valuesJsArray)
  }
}
