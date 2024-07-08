package software.altitude.core.controllers.htmx

import play.api.libs.json.JsObject
import software.altitude.core.DataScrubber
import software.altitude.core.DuplicateException
import software.altitude.core.ValidationException
import software.altitude.core.Validators.ApiRequestValidator
import software.altitude.core.controllers.BaseHtmxController
import software.altitude.core.models.Folder
import software.altitude.core.{Const => C}

class UserInterfaceHtmxController extends BaseHtmxController{

  get("/modals/add-folder") {
    val parentId: String = params.get(C.Api.Folder.PARENT_ID).get

    ssp("htmx/add_folder_modal",
      "minWidth" -> C.UI.ADD_FOLDER_MODAL_MIN_WIDTH,
      "title" -> C.UI.ADD_FOLDER_MODAL_TITLE,
      C.Api.Folder.PARENT_ID -> parentId)
  }

  get("/modals/rename-folder") {
    val folderId: String = params.get(C.Api.ID).get

    val folder: Folder = app.service.folder.getById(folderId)

    ssp("htmx/rename_folder_modal",
      "minWidth" -> C.UI.RENAME_FOLDER_MODAL_MIN_WIDTH,
      "title" -> C.UI.RENAME_FOLDER_MODAL_TITLE,
      C.Api.Folder.EXISTING_NAME -> Some(folder.name),
      C.Api.ID -> folderId)
  }

  post("/folder/add") {
    val dataScrubber = DataScrubber(
      trim = List(C.Api.Folder.NAME),
    )

    val apiRequestValidator = ApiRequestValidator(
      required = List(
        C.Api.Folder.NAME, C.Api.Folder.PARENT_ID),
      maxLengths = Map(
        C.Api.Folder.NAME -> C.Api.Constraints.MAX_FOLDER_NAME_LENGTH,
      ),
      minLengths = Map(
        C.Api.Folder.NAME -> C.Api.Constraints.MIN_FOLDER_NAME_LENGTH,
      ),
    )

    val jsonIn: JsObject = dataScrubber.scrub(unscrubbedReqJson.get)

    def haltWithValidationErrors(errors: Map[String, String], parentId: String): Unit = {
      halt(200,
        ssp(
          "htmx/add_folder_modal",
          "minWidth" -> C.UI.ADD_FOLDER_MODAL_MIN_WIDTH,
          "title" -> C.UI.ADD_FOLDER_MODAL_TITLE,
          "fieldErrors" -> errors,
          "formJson" -> jsonIn,
          C.Api.Folder.PARENT_ID -> parentId
        ),
        // we want to change the folder modal to show the errors, not reload the folder list!
        headers=Map("HX-Retarget" -> "this", "HX-Reswap" -> "innerHTML")
      )
    }

    try {
      apiRequestValidator.validate(jsonIn)
    } catch {
      case validationException: ValidationException =>
        haltWithValidationErrors(
          validationException.errors.toMap,
          parentId = (jsonIn \ C.Api.Folder.PARENT_ID).as[String])
    }

    val folderName = (jsonIn \ C.Api.Folder.NAME).as[String]
    val parentId = (jsonIn \ C.Api.Folder.PARENT_ID).as[String]

    try {
      app.service.library.addFolder(folderName, parentId = Some(parentId))
    } catch {
      case _: DuplicateException =>
        haltWithValidationErrors(Map(C.Api.Folder.NAME -> "Folder name already exists at this level"), parentId = parentId)
    }

    val childFolders: List[Folder] = app.service.folder.immediateChildren(parentId)

    halt(200,
      ssp("htmx/folder_children.ssp",
        "folders" -> childFolders)
    )
  }

  put("/folder/rename") {
    val dataScrubber = DataScrubber(
      trim = List(C.Api.Folder.NAME),
    )

    val apiRequestValidator = ApiRequestValidator(
      required = List(
        C.Api.Folder.NAME, C.Api.ID),
      maxLengths = Map(
        C.Api.Folder.NAME -> C.Api.Constraints.MAX_FOLDER_NAME_LENGTH,
      ),
      minLengths = Map(
        C.Api.Folder.NAME -> C.Api.Constraints.MIN_REPOSITORY_NAME_LENGTH,
      ),
    )

    val jsonIn: JsObject = dataScrubber.scrub(unscrubbedReqJson.get)

    def haltWithValidationErrors(errors: Map[String, String], folderId: String): Unit = {
      halt(200,
        ssp(
          "htmx/rename_folder_modal",
          "minWidth" -> C.UI.RENAME_FOLDER_MODAL_MIN_WIDTH,
          "title" -> C.UI.RENAME_FOLDER_MODAL_TITLE,
          "fieldErrors" -> errors,
          "formJson" -> jsonIn,
          C.Api.ID -> folderId
        ),
        // we want to change the folder modal to show the errors, not reload the folder list!
        headers=Map("HX-Retarget" -> "this", "HX-Reswap" -> "innerHTML")
      )
    }

    try {
      apiRequestValidator.validate(jsonIn)
    } catch {
      case validationException: ValidationException =>
        haltWithValidationErrors(
          validationException.errors.toMap,
          folderId = (jsonIn \ C.Api.ID).as[String])
    }

    val newName = (jsonIn \ C.Api.Folder.NAME).as[String]
    val folderId = (jsonIn \ C.Api.ID).as[String]

    try {
      app.service.library.renameFolder(folderId=folderId, newName=newName)
    } catch {
      case _: DuplicateException =>
        haltWithValidationErrors(
          Map(C.Api.Folder.NAME -> "Folder name already exists at this level"), folderId = folderId)
    }

    halt(200, newName)
  }

  /**
   * WARNING - the closing of a context menu is done via JS beforeRequest event, and it looks
   * for this *URL*, so changing this will break the CLOSE functionality.
   */
  get("/folder/context-menu") {
    val folderId: String = params.get("folderId").get

    ssp("htmx/folder_context_menu",
      "folderId" -> folderId)
  }

  get("/folder/children") {
    val parentId: String = params.get(C.Api.Folder.PARENT_ID).get
    val childFolders: List[Folder] = app.service.folder.immediateChildren(parentId)

    ssp("htmx/folder_children",
      "folders" -> childFolders)
  }

  // This just clears the body of the modal
  get("/close-modal") {
    ssp("htmx/close_modal")
  }

  post("/folder/move") {
    request.parameters.foreach(println)
    halt(200, "Folder moved")
  }
}
