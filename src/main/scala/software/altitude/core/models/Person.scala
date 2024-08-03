package software.altitude.core.models

import play.api.libs.json._
import software.altitude.core.{Const => C}

import scala.language.implicitConversions

object Person {
  val UNKNOWN_NAME_PREFIX = "Unknown"
  // Number of labels reserved for special cases, and not used for actual people instances
  // Labels start at this number + 1 but Unknown people start at 1 (so reserved label count must be known)
  val RESERVED_LABEL_COUNT = 10

  implicit def fromJson(json: JsValue): Person = {

    Person(
      id = (json \ C.Base.ID).asOpt[String],
      name = (json \ C.Person.NAME).asOpt[String],
      coverFaceId = (json \ C.Person.COVER_FACE_ID).asOpt[String],
      label = (json \ C.Person.LABEL).as[Long],
      mergedWithIds = (json \ C.Person.MERGED_WITH_IDS).as[List[String]],
      mergedIntoId = (json \ C.Person.MERGED_INTO_ID).asOpt[String],
      numOfFaces = (json \ C.Person.NUM_OF_FACES).as[Int],
      isHidden = (json \ C.Person.IS_HIDDEN).as[Boolean]
    ).withCoreAttr(json)
  }
}

case class Person(id: Option[String] = None,
                  name: Option[String] = None,
                  coverFaceId: Option[String] = None,
                  mergedWithIds: List[String] = List(),
                  label: Long = -1,
                  numOfFaces: Int = 0,
                  mergedIntoId: Option[String] = None,
                  isHidden: Boolean = false) extends BaseModel {

  override def toJson: JsObject = {
    Json.obj(
      C.Person.LABEL -> label,
      C.Person.NAME -> name,
      C.Person.COVER_FACE_ID -> coverFaceId,
      C.Person.NUM_OF_FACES -> numOfFaces,
      C.Person.MERGED_WITH_IDS -> mergedWithIds,
      C.Person.MERGED_INTO_ID -> mergedIntoId,
      C.Person.IS_HIDDEN -> isHidden,
    ) ++ coreJsonAttrs
  }

  override def canEqual(other: Any): Boolean = other.isInstanceOf[Face]

  override def equals(that: Any): Boolean = that match {
    case that: Person if !that.canEqual( this) => false
    case that: Person => that.id == this.id
    case _ => false
  }

  override def hashCode: Int = super.hashCode
}
