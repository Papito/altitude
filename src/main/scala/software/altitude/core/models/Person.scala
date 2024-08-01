package software.altitude.core.models

import play.api.libs.json._
import software.altitude.core.{Const => C}

import scala.language.implicitConversions

object Person {
  implicit def fromJson(json: JsValue): Person = {

    Person(
      id = (json \ C.Base.ID).asOpt[String],
      name = (json \ C.Person.NAME).as[String],
      label = (json \ C.Person.LABEL).as[Long],
      mergedWithIds = (json \ C.Person.MERGED_WITH_IDS).as[Seq[String]],
      numOfFaces = (json \ C.Person.NUM_OF_FACES).as[Int],
      mergedIntoId = (json \ C.Person.MERGED_INTO_ID).asOpt[String],
      isHidden = (json \ C.Person.IS_HIDDEN).as[Boolean]
    ).withCoreAttr(json)
  }
}

case class Person(id: Option[String] = None,
                  name: String,
                  mergedWithIds: Seq[String] = List(),
                  label: Long = -1,
                  numOfFaces: Int = 0,
                  mergedIntoId: Option[String] = None,
                  isHidden: Boolean = false) extends BaseModel {

  override def toJson: JsObject = {
    Json.obj(
      C.Person.LABEL -> label,
      C.Person.NAME -> name,
      C.Person.NUM_OF_FACES -> numOfFaces,
      C.Person.MERGED_WITH_IDS -> mergedWithIds,
      C.Person.MERGED_INTO_ID -> numOfFaces,
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
