package altitude.models

import altitude.{Const => C}
import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat
import play.api.libs.json._
import scala.language.implicitConversions

object Trash {
  implicit def fromJson(json: JsValue): Trash = {

    val trash = new Trash(
      id = (json \ C("Base.ID")).asOpt[String],
      mediaType = json \ C("Asset.MEDIA_TYPE"),
      path = (json \ C("Asset.PATH")).as[String],
      folderId = (json \ C("Asset.FOLDER_ID")).as[String],
      md5 = (json \ C("Asset.MD5")).as[String],
      sizeBytes = (json \ C("Asset.SIZE_BYTES")).as[Long],
      metadata = json \ C("Asset.METADATA")
    ).withCoreAttr(json)

    val isoRecycledAt = (json \ C("Trash.RECYCLED_AT")).asOpt[String]
    if (isoRecycledAt.isDefined) {
      trash.recycledAt = ISODateTimeFormat.dateTime().parseDateTime(isoRecycledAt.get)
    }

    trash
  }
}

class Trash(override val id: Option[String] = None,
            override val mediaType: MediaType,
            override val path: String,
            override val md5: String,
            override val sizeBytes: Long,
            override val folderId: String,
            override val metadata: JsValue = JsNull,
            override val previewData: Array[Byte] = new Array[Byte](0)) extends Asset(id = id,
                                                                                      mediaType = mediaType,
                                                                                      path = path,
                                                                                      md5 = md5,
                                                                                      sizeBytes = sizeBytes,
                                                                                      folderId = folderId,
                                                                                      metadata = metadata,
                                                                                      previewData = previewData) {
  // created at
  protected var _recycledAt: Option[DateTime] = None

  def recycledAt = _recycledAt

  def recycledAt_= (arg: DateTime): Unit = {
    if (_recycledAt.isDefined)
      throw new RuntimeException("Cannot set 'recycled at' twice")
    _recycledAt = Some(arg)
  }

}