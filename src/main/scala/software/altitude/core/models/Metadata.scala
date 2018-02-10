package software.altitude.core.models

import play.api.libs.json._

import scala.language.implicitConversions

object Metadata {
  implicit def fromJson(json: JsObject): Metadata = {
    val data = json.keys.foldLeft(Map[String, Set[MetadataValue]]()) { (res, fieldId) =>
      val valuesJson = (json \ fieldId).as[List[JsValue]]
      valuesJson.map(MetadataValue.fromJson)
      res + (fieldId -> valuesJson.map(MetadataValue.fromJson).toSet)
    }

    new Metadata(data)
  }

  /**
   * Adapter to easily set metadata from a set of plain strings.
   * Note: dummy implicit is added to prevent compiler from complaining about double defition
   * due to type erasure
   * @param data { fieldId -> Set of string }
   */
  def apply(data: Map[String, Set[String]])(implicit d: DummyImplicit): Metadata = {
    val convertedData: Map[String, Set[MetadataValue]] = data.foldLeft(Map[String, Set[MetadataValue]]()) {
      case (a, (fieldId, strValues)) =>
        a ++ Map[String, Set[MetadataValue]](fieldId -> strValues.map(MetadataValue.apply))
    }

    Metadata(convertedData)
  }

  def apply(): Metadata = {
    Metadata(Map[String, Set[MetadataValue]]())
  }

  def withIds(metadata: Metadata): Metadata = {
    val dataWithIds = metadata.data.map{ case (fieldId, mValues) =>
      val mValuesWithIds = mValues.map{ mValue =>
        mValue.id match {
          case None => MetadataValue(id = Some(BaseModel.genId), value = mValue.value)
          case Some(_) => mValue
        }
      }
      (fieldId, mValuesWithIds)
    }

    Metadata(dataWithIds)
  }
}

case class Metadata(data: Map[String, Set[MetadataValue]])
  extends BaseModel with NoId {

  def get(key: String): Option[Set[MetadataValue]] = data.get(key)
  def apply(key: String) = data(key)
  def contains(key: String) = data.keys.toSeq.contains(key)
  def isEmpty = data.isEmpty

  override val toJson = data.foldLeft(Json.obj()) { (res, m) =>
    val fieldId = m._1

    val valuesJsArray: JsArray = JsArray(m._2.toSeq.map(_.toJson))

    // append to the resulting JSON object
    res ++ Json.obj(fieldId -> valuesJsArray)
  }
}
