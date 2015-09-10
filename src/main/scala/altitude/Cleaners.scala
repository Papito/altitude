package altitude

import play.api.libs.json.{JsObject, Json}

object Cleaners {
  case class Cleaner(trim: Option[List[String]] = None,
                     lower:  Option[List[String]] = None,
                     defaults:  Option[Map[String, String]] = None) {

    def clean(json: JsObject): JsObject = {
      doLower(doDefaults(doTrim(json)))
    }

    protected def doTrim(json: JsObject): JsObject = {
      trim.getOrElse(List[String]()).fold(json) { (res, field) =>
        (json \ field.toString).asOpt[String] match {
          case v: Some[String] if v.nonEmpty => json ++ Json.obj(field.toString -> v.get.trim)
          case _ => json
        }
      }.asInstanceOf[JsObject]
    }

    protected def doLower(json: JsObject): JsObject = {
      lower.getOrElse(List[String]()).fold(json) { (res, field) =>
        (json \ field.toString).asOpt[String] match {
          case v: Some[String] if v.nonEmpty => json ++ Json.obj(field.toString -> v.get.toLowerCase)
          case _ => json
        }
      }.asInstanceOf[JsObject]
    }

    protected def doDefaults(json: JsObject): JsObject = {
      defaults.getOrElse(Map[String, String]()).keys.fold(json) { (res, field) =>
        (json \ field.toString).asOpt[String] match {
          case v: Some[String] if v.isEmpty => json ++ Json.obj(
            field.toString -> defaults.get(field.toString) )
          case _ => json
        }
      }.asInstanceOf[JsObject]
    }
  }
}
