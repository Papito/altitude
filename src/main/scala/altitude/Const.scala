package altitude


object Const {
  /*---------------------------------------------------------------------------
  MODELS
  ---------------------------------------------------------------------------*/
  trait Common {
    val ID = "id"
    val VALUES = "values"
    val DATA = "data"
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
    val MIME_TYPE = "mime_type"
  }

  object ImportProfile extends Common {
    val NAME = "name"
    val TAG_DATA = "tag_data"
  }

  object Tag extends Common {
    val NAME = "name"
    val TYPE = "type"
    val MAX_LENGTH = "max_length"
    val ALLOWS_MULTI = "allows_multi"
    val RESTRICTED_VALUE_LIST = "restricted_value_list"
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
    val DATA = "data"
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

    object TagConfig {
      val TAG_CONFIG = "tagConfig"
    }
  }

  /*---------------------------------------------------------------------------
  MESSAGES
  ---------------------------------------------------------------------------*/
  val MSG: Map[String, String] = Map(
    "warn.duplicate" -> "Duplicate",
    "err.required" -> "This field is required",
    "err.validation_errors" -> "There are validation errors in: %s",
    "err.wrong_type" -> "This field does not match the required type (%s)"
  )

  /*---------------------------------------------------------------------------
  LOG TAGS
  ---------------------------------------------------------------------------*/
  object LogTag {
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
