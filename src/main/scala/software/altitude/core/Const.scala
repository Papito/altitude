package software.altitude.core


object Const {

  /**
   * TYPES
   */
  object DatasourceType extends Enumeration {
    val SQLITE, POSTGRES = Value
  }

  object FileStoreType extends Enumeration {
    val FS, S3 = Value
  }

  object ImportMode extends Enumeration {
    val COPY, MOVE, COPY_AND_LANDFILL = Value
  }

  /**
   * MODEL FIELDS (in JSON and database)
   */
  trait Common {
    val ID = "id"
    val REPO_ID = "repository_id"
    val USER_ID = "user_id"
    val VALUES = "values"
    val DATA = "data"
    val CREATED_AT = "created_at"
    val UPDATED_AT = "updated_at"
    val IS_CLEAN = "is_clean"
  }

  object Base extends Common

  object Repository extends Common {
    val NAME = "name"
    val ROOT_FOLDER_ID = "root_folder_id"
    val TRIAGE_FOLDER_ID = "triage_folder_id"
    val FILE_STORE_TYPE = "file_store_type"
    val FILES_STORE_CONFIG = "file_store_config"

    object Config {
      val PATH = "path"
    }
  }

  object User extends Common {
  }

  object Asset extends Common {
    val ASSET_TYPE = "asset_type"
    val PATH = "path"
    val FOLDER_ID = "folder_id"
    val MD5 = "md5"
    val SIZE_BYTES = "size_bytes"
    val FILENAME = "filename"
    val METADATA = "metadata"
    val EXTRACTED_METADATA = "extracted_metadata"
    val IS_RECYCLED = "is_recycled"
  }

  object AssetType extends Common {
    val MIME_TYPE = "mime_type"
    val MEDIA_TYPE = "media_type"
    val MEDIA_SUBTYPE = "media_subtype"
  }

  object Folder extends Common {
    val NAME = "name"
    val NAME_LC = "name_lc"
    val PARENT_ID = "parent_id"
    val PATH = "path"
    val NUM_OF_ASSETS = "num_of_assets"
    val CHILDREN = "children"
    val IS_ROOT = "is_root"

    object Names {
      val ROOT = "root"
      val TRIAGE = "triage"
    }
  }

  object Preview {
    val ASSET_ID = "asset_id"
    val MIME_TYPE = "mime_type"
    val DATA = "data"
  }

  object Data {
    val ASSET_ID = "asset_id"
    val MIME_TYPE = "mime_type"
    val DATA = "data"
  }

  object Stat extends Common {
    val DIMENSION = "dimension"
    val DIM_VAL = "dim_val"
  }

  object MetadataField extends Common {
    val NAME = "name"
    val NAME_LC = "name_lc"
    val FIELD_TYPE = "field_type"
  }

  object SearchToken extends Common {
    val ASSET_ID = "asset_id"
    val FIELD_ID = "field_id"
    val FIELD_VALUE_KW = "field_value_kw"
    val FIELD_VALUE_NUM = "field_value_num"
    val FIELD_VALUE_BOOL = "field_value_bool"
    val FIELD_VALUE_DT = "field_value_dt"
  }

  object Api {
    val USER_ID = "user_id"
    val ERROR = "error"
    val STACKTRACE = "stacktrace"
    val WARNING = "warning"
    val CRITICAL = "critical"
    val VALIDATION_ERROR = "validation_error"
    val VALIDATION_ERRORS = "validation_errors"
    val MULTI_VALUE_DELIM = "+"
    val DUPLICATE_OF = "duplicate_of"

    // FIXME: wrong naming convention
    val TOTAL_RECORDS = "totalRecords"
    val TOTAL_PAGES = "totalPages"
    val CURRENT_PAGE = "currentPage"
    val RESULTS_PER_PAGE = "resultsPerPage"

    val ID = "id"
    val DATA = "data"
    val PATH = "path"
    val DIRECTORY_NAMES = "directory_names"
    val OS_DIR_SEPARATOR = "os_dir_separator"
    val CURRENT_PATH = "current_path"

    object Import {
      val IMPORTED = "imported"
    }

    object ImportAsset {
      val IMPORT_ASSET = "import_asset"
    }

    object Asset {
      val ASSET = "asset"
      val FOLDER_ID = "folder_id"
      val ASSETS = "assets"
      val METADATA = "metadata"
      val METADATA_FIELD_ID = "metadata_field_id"
    }

    object Search {
      val ASSETS = "assets"
      val QUERY_TEXT = "query_txt"
      val RESULTS_PER_PAGE = "rpp"
      val PAGE = "p"
      val FOLDERS = "folders"
    }

    object Folder {
      val QUERY_ARG_NAME = "__FOLDERS__"
      val HIERARCHY = "hierarchy"
      val SYSTEM = "system"
      val TRIAGE = "triage"
      val TRASH = "trash"
      val FOLDERS = "folders"
      val FOLDER = "folder"
      val ASSET_IDS = "asset_ids"
      val PATH = "path"
      val NAME = "name"
      val PARENT_ID = "parent_id"
    }

    object Trash {
      val ASSET_IDS = "asset_ids"
    }

    object Stats {
      val STATS = "stats"
    }

    object Metadata {
      val FIELDS = "fields"
      val VALUE = "value"

      object Field {
        val NAME = "name"
        val TYPE = "type"
      }
    }
  }

  object Msg {

    object Warn {
      val DUPLICATE = "Duplicate"
      val DUPLICATE_FIELD_VALUE = "Duplicate values in [%s]"
      val REQUIRED = "This field is required"
      val INCORRECT_VALUE_TYPE = "Incorrect value type for [%s]"
    }

    object Err {
      val CANNOT_BE_EMPTY = "This field cannot be empty"
      val VALUE_TOO_LONG = "The value cannot be more than %s characters long"
      val VALIDATION_ERROR = "Validation error"
      val VALIDATION_ERRORS = "There are validation errors in: %s"
      val WRONG_TYPE = "This field does not match the required type (%s)"
      val WRONG_VALUE = "This is not an allowed value. Allowed values are: %s"
      val EMPTY_REQUEST_BODY = "Empty request body"
    }
  }

  /**
   * Paths for major data partitions, relative to root path
   */
  object Path {
    val ROOT = "root"
    val TRIAGE = "triage"
    val TRASH = "trash"
    val LANDFILL = "landfill"
  }

  // default results per page
  final val DEFAULT_RPP = "20"

  object LogTag {
    val APP     = "APP"
    val API     = "API"
    val DAO     = "DAO"
    val WEB     = "WEB"
    val SERVICE = "SERVICE"
    val DB = "DB"
  }

}
