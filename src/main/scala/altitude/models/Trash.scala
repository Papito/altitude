package altitude.models

import play.api.libs.json._
import scala.language.implicitConversions

object Trash {
  implicit def fromJson(json: JsValue): Trash = Asset.fromJson(json).asInstanceOf[Trash]
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
                                                                                      previewData = previewData)
