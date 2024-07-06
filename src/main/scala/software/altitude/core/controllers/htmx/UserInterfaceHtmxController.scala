package software.altitude.core.controllers.htmx

import play.api.libs.json.JsObject
import software.altitude.core.DataScrubber
import software.altitude.core.DuplicateException
import software.altitude.core.RequestContext
import software.altitude.core.ValidationException
import software.altitude.core.Validators.ApiRequestValidator
import software.altitude.core.controllers.BaseHtmxController
import software.altitude.core.models.Folder
import software.altitude.core.{Const => C}

class UserInterfaceHtmxController extends BaseHtmxController{

  get("/modals/add-folder") {
    ssp("htmx/add_folder_modal",
      "minWidth" -> C.UI.ADD_FOLDER_MODAL_MIN_WIDTH,
      "title" -> C.UI.ADD_FOLDER_MODAL_TITLE)
  }

  post("/folder/add") {
    val dataScrubber = DataScrubber(
      trim = List(C.Api.Folder.NAME),
    )

    val apiRequestValidator = ApiRequestValidator(
      required = List(
        C.Api.Folder.NAME),
      maxLengths = Map(
        C.Api.Folder.NAME -> C.Api.Constraints.MAX_FOLDER_NAME_LENGTH,
      ),
      minLengths = Map(
        C.Api.Folder.NAME -> C.Api.Constraints.MIN_REPOSITORY_NAME_LENGTH,
      ),
    )

    val jsonIn: JsObject = dataScrubber.scrub(unscrubbedReqJson.get)

    def haltWithValidationErrors(errors: Map[String, String]): Unit = {
      halt(200,
        ssp(
          "htmx/add_folder_modal",
          "minWidth" -> C.UI.ADD_FOLDER_MODAL_MIN_WIDTH,
          "title" -> C.UI.ADD_FOLDER_MODAL_TITLE,
          "fieldErrors" -> errors,
          "formJson" -> jsonIn),
        // we want to change the folder modal to show the errors, not reload the folder list!
        headers=Map("HX-Retarget" -> "this", "HX-Reswap" -> "innerHTML")
      )
    }

    try {
      apiRequestValidator.validate(jsonIn)
    } catch {
      case validationException: ValidationException =>
        haltWithValidationErrors(validationException.errors.toMap)
    }

    val repo = RequestContext.getRepository
    val folderName = (jsonIn \ C.Api.Folder.NAME).as[String]

    try {
      app.service.library.addFolder(folderName)
    } catch {
      case _: DuplicateException =>
        haltWithValidationErrors(Map(C.Api.Folder.NAME -> "Folder name already exists."))
    }

    val firstLevelFolders: List[Folder] = app.service.folder.immediateChildren(repo.rootFolderId)

    halt(200,
      ssp("htmx/folders.ssp", "folders" -> firstLevelFolders)
    )
  }

  /**
   * WARNING - the closing of a context menu is done via JS beforeRequest event, and it looks
   * for this *URL*, so changing this will break the CLOSE functionality.
   */
  get("/folder/context-menu") {
    val folderId: String = params.get("folderId").get

    ssp("htmx/folder_context_menu", "folderId" -> folderId)
  }

  // This just clears the body of the modal
  get("/close-modal") {
    ssp("htmx/close_modal")
  }
}
