package altitude


object Const {

  def apply(key: String): String = {
    val v = data.get(key)
    if (v.isDefined) v.get else throw new RuntimeException(s"No constant value for: $key")
  }

  val data = Map(
    "Base.ID" -> "id",
    "Base.VALUES" -> "values",
    "Base.DATA" -> "data",
    "Base.CREATED_AT" -> "created_at",
    "Base.UPDATED_AT" -> "updated_at",
    "Base.IS_CLEAN" -> "is_clean",
    "System.UNCATEGORIZED_COUNT" ->  "uncategorized_count",
    "System.TRASH_COUNT" -> "trash_count",

    /*---------------------------------------------------------------------------
    MODELS
    ---------------------------------------------------------------------------*/
    "Asset.MEDIA_TYPE" -> "media_type",
    "Asset.PATH" -> "path",
    "Asset.FOLDER_ID" -> "folder_id",
    "Asset.MD5" -> "md5",
    "Asset.SIZE_BYTES" -> "size_bytes",
    "Asset.FILENAME" -> "filename",
    "Asset.MEDIA_SUBTYPE" -> "media_subtype",
    "Asset.MIME_TYPE" -> "mime_type",
    "Asset.METADATA" -> "metadata",
    "Folder.NAME" -> "name",
    "Folder.NAME_LC" -> "name_lc",
    "Folder.PARENT_ID" -> "parent_id",
    "Folder.NUM_OF_ASSETS" -> "num_of_assets",
    "Folder.CHILDREN" -> "children",
    "Folder.IS_ROOT" -> "is_root",
    "Folder.IS_UNCATEGORIZED" -> "is_uncategorized",
    "Folder.IS_SYSTEM" -> "is_system",
    "Folder.Ids.ROOT" -> "0",
    "Folder.Ids.UNCATEGORIZED" -> "1",
    "Folder.Ids.TRASH" -> "2",
    "Folder.Names.ROOT" -> "All", //TODO: not i18n
    "Folder.Names.UNCATEGORIZED" -> "Uncategorized",  //TODO: not i18n
    "Folder.Names.TRASH" ->  "Trash",  //TODO: not i18n
    "Preview.ASSET_ID" -> "asset_id",
    "Preview.MIME_TYPE" -> "mime_type",
    "Stats.TOTAL_ASSETS" -> "total_assets",
    "Stats.TOTAL_BYTES" -> "total_bytes",
    "Stats.UNCATEGORIZED_ASSETS" -> "uncategorized_assets",
    "Stats.RECYCLED_ASSETS" -> "recycled_assets",
    "Stats.RECYCLED_BYTES" -> "recycled_bytes",

    /*---------------------------------------------------------------------------
    API
    ---------------------------------------------------------------------------*/
    "Api.ERROR" -> "error",
    "Api.STACKTRACE" -> "stacktrace",
    "Api.WARNING" -> "warning",
    "Api.CRITICAL" -> "critical",
    "Api.VALIDATION_ERROR" -> "validation_error",
    "Api.VALIDATION_ERRORS" -> "validation_errors",
    "Api.MULTI_VALUE_DELIM" -> "+",
    "Api.DUPLICATE_OF" -> "duplicate_of",

    "Api.TOTAL_RECORDS" -> "totalRecords",
    "Api.TOTAL_PAGES" -> "totalPages",
    "Api.CURRENT_PAGE" -> "currentPage",
    "Api.RESULTS_PER_PAGE" -> "resultsPerPage",

    "Api.ID" -> "id",
    "Api.DATA" -> "data",
    "Api.PATH" -> "path",
    "Api.DIRECTORY_NAMES" -> "directory_names",
    "Api.CURRENT_PATH" -> "current_path",
    "Api.Import.IMPORTED" -> "imported",
    "Api.ImportAsset.IMPORT_ASSET" -> "import_asset",
    "Api.Asset.ASSET" -> "asset",
    "Api.Asset.FOLDER_ID" -> "folder_id",
    "Api.Asset.ASSETS" -> "assets",
    "Api.Search.ASSETS" -> "assets",
    "Api.Search.QUERY_TEXT" -> "query_txt",
    "Api.Search.RESULTS_PER_PAGE" -> "rpp",
    "Api.Search.PAGE" -> "p",
    "Api.Search.FOLDERS" -> "folders",
    "Api.Folder.QUERY_ARG_NAME" -> "__FOLDERS__",
    "Api.Folder.HIERARCHY" -> "hierarchy",
    "Api.Folder.SYSTEM" -> "system",
    "Api.Folder.UNCATEGORIZED" -> "uncategorized",
    "Api.Folder.TRASH" -> "trash",
    "Api.Folder.FOLDERS" -> "folders",
    "Api.Folder.FOLDER" -> "folder",
    "Api.Folder.ASSET_IDS" -> "asset_ids",
    "Api.Folder.PATH" -> "path",
    "Api.Folder.NAME" -> "name",
    "Api.Folder.PARENT_ID" -> "parent_id",

    /*---------------------------------------------------------------------------
    MESSAGES
    ---------------------------------------------------------------------------*/
    "msg.warn.duplicate" -> "Duplicate",
    "msg.err.required" -> "This field is required",
    "msg.err.validation_error" -> "Validation error",
    "msg.err.validation_errors" -> "There are validation errors in: %s",
    "msg.err.wrong_type" -> "This field does not match the required type (%s)",
    "msg.err.empty_request_body" -> "Empty request body"
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
