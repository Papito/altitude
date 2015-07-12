package altitude


object Const {
   object tag {
     val APP     = "APP"
     val API     = "API"
     val WEB     = "WEB"
     val SERVICE = "SERVICE"
     val DB = "DB"
   }

   trait Common {
     val ID = "id"
     val CREATED_AT = "created_at"
     val UPDATED_AT = "updated_at"
   }

  object Base extends Common

  object Asset extends Common {
    val MEDIA_TYPE = "media_type"
    val PATH = "path"
    val MD5 = "md5"
    val SIZE_BYTES = "size_bytes"
    val FILENAME = "filename"
    val MEDIA_SUBTYPE = "media_subtype"
    val MIME_TYPE = "mime_type"
    val METADATA = "metadata"
    // FIXME: remove
    val IMAGE_PREVIEW = "image_preview"
  }

  object Preview extends Common {
    val DATA = "data"
    val MIME = "mime"
  }

  object Metadata extends Common
 }
