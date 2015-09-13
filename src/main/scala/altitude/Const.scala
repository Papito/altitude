package altitude


object Const {
  /*---------------------------------------------------------------------------
  MODELS
  ---------------------------------------------------------------------------*/
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
  }

  object Preview extends Common {
    val ASSET_ID =  "asset_id"
    val DATA = "data"
    val MIME_TYPE = "mime_type"
  }

  object ImportProfile extends Common {
    val NAME = "name"
    val KEYWORDS = "keywords"
  }

  object UserMetaField extends Common {
    val NAME = "name"
    val TYPE = "type"
    val MAX_LENGTH = "maxLength"
    val ALLOWS_MULTI = "allowsMulti"
    val RESTRICTED_VALUE_LIST = "restrictedValueList"
  }

  /*---------------------------------------------------------------------------
  API
  ---------------------------------------------------------------------------*/
  object Api {
    val ERROR = "error"
    val WARNING = "warning"
    val CRITICAL = "critical"
    val VALIDATION_ERRORS = "validationErrors"

    val ID = "id"
    val PATH = "path"
    val DIRECTORY_NAMES = "directoryNames"
    val CURRENT_PATH = "currentPath"

    object ImportAsset {
      val IMPORT_ASSET = "importAsset"
    }

    object Asset {
      val ASSET = "asset"
      val ASSETS = "assets"
    }

    object ImportProfile {
      val IMPORT_PROFILES = "importProfiles"
      val NAME = "name"
      val KEYWORDS = "keywords"
    }

    object Search {
      val ASSETS = "assets"
    }
  }

  /*---------------------------------------------------------------------------
  MESSAGES
  ---------------------------------------------------------------------------*/
  val MSG: Map[String, String] = Map(
    "warn.duplicate" -> "Duplicate",
    "err.required" -> "This field is required"
  )

  /*---------------------------------------------------------------------------
  LOG TAGS
  ---------------------------------------------------------------------------*/
  object tag {
    val APP     = "APP"
    val API     = "API"
    val WEB     = "WEB"
    val SERVICE = "SERVICE"
    val DB = "DB"
  }

  /*---------------------------------------------------------------------------
  MISC
  ---------------------------------------------------------------------------*/
  object IdType {
    val ID = "ID"
    val QUERY = "QUERY"
  }

}
