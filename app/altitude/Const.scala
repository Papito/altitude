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
   }
  object Base extends Common

  object Asset extends Common {
    val MEDIA_TYPE = "media_type"
    val LOCATIONS = "locations"
    val MEDIA_SUBTYPE = "media_subtype"
    val MIME_TYPE = "mime_type"
    val METADATA = "metadata"
  }

  object Storage extends Common {
    val NAME = "name"
    val DESCRIPTION = "description"
    val TYPE = "type"
  }

  object StorageLocation extends Common {
    val STORAGE_ID = "storage_id"
    val PATH = "path"
  }
  object Metadata extends Common
 }
