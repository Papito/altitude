package altitude.models

import altitude.{Const => C, Util}
import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat
import play.api.libs.json._

import scala.language.implicitConversions

object Trash {
  implicit def fromJson(json: JsValue): Trash = {

    val trash = new Trash(
      id = (json \ C("Base.ID")).asOpt[String],
      userId = (json \ C("Base.USER_ID")).as[String],
      assetType = json \ C("Asset.ASSET_TYPE"),
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
            override val userId: String,
            override val assetType: AssetType,
            override val path: String,
            override val md5: String,
            override val sizeBytes: Long,
            override val folderId: String,
            override val metadata: JsValue = JsNull,
            override val previewData: Array[Byte] = new Array[Byte](0)) extends Asset(id = id,
                                                                                      userId,
                                                                                      assetType = assetType,
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

  override def toJson = super.toJson ++ Json.obj(
    C("Trash.RECYCLED_AT") -> {recycledAt match {
      case None => JsNull
      case _ => JsString(Util.isoDateTime(recycledAt))
  }})
}