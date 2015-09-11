package altitude

import play.api.libs.json.{JsObject, Json}

object Cleaners {
  case class Cleaner(trim: Option[List[String]] = None,
                     lower:  Option[List[String]] = None,
                     defaults:  Option[Map[String, String]] = None) {

    def clean(json: JsObject): JsObject = {
.      doLower(doDefaults(doTrim(json)))
    }

    protected def doTrim(json: JsObject): JsObject = {
      trim.getOrElse(List[String]()).foldLeft(Json.obj()) { (res, field) =>
        (json \ field.toString).asOpt[String] match {
          case v: Some[String] if v.nonEmpty => res ++ Json.obj(field.toString -> v.get.trim)
          case _ => json
        }
      }
    }

    protected def doLower(json: JsObject): JsObject = {
      lower.getOrElse(List[String]()).foldLeft(Json.obj()) { (res, field) =>
        (json \ field.toString).asOpt[String] match {
          case v: Some[String] if v.nonEmpty => res ++ Json.obj(field.toString -> v.get.toLowerCase)
          case _ => json
        }
      }
    }

    protected def doDefaults(json: JsObject): JsObject = {
      defaults.getOrElse(Map[String, String]()).keys.foldLeft(Json.obj()) { (res, field) =>
        (json \ field.toString).asOpt[String] match {
          case v: Some[String] if v.isEmpty => res ++ Json.obj(
            field.toString -> defaults.get(field.toString) )
          case _ => json
        }
      }
    }
  }
}
