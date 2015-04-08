package altitude

import play.api.libs.json.Json

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
   }
  object Base extends Common

  object Asset extends Common {
    val MEDIA_TYPE = "media_type"
    val PATH = "path"
    val MEDIA_SUBTYPE = "media_subtype"
    val MIME_TYPE = "mime_type"
  }

  object Metadata extends Common

  object Db {
    val EMPTY: Int = Json.obj().hashCode()

  }
 }
