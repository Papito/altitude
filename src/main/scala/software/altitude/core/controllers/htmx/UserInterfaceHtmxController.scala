package software.altitude.core.controllers.htmx

import play.api.libs.json.JsObject
import software.altitude.core.{DataScrubber, RequestContext, ValidationException, Const => C}
import software.altitude.core.Validators.ApiRequestValidator
import software.altitude.core.controllers.BaseHtmxController
import software.altitude.core.models.Folder

class UserInterfaceHtmxController extends BaseHtmxController{

  private val FOLDER_MODAL_MIN_WIDTH = 400

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

    try {
      apiRequestValidator.validate(jsonIn)
    } catch {
      case validationException: ValidationException =>
        println(validationException.errors.toMap)
        halt(200,
          ssp(
          "htmx/add_folder_modal",
          "minWidth" -> C.UI.ADD_FOLDER_MODAL_MIN_WIDTH,
          "title" -> C.UI.ADD_FOLDER_MODAL_TITLE,
          "fieldErrors" -> validationException.errors.toMap,
          "formJson" -> jsonIn),
          // we want to change the folder modal to show the errors, not reload the folder list!
          headers=Map("HX-Retarget" -> "this", "HX-Reswap" -> "innerHTML")
        )
    }

    val repo = RequestContext.getRepository

    val folderName = (jsonIn \ C.Api.Folder.NAME).as[String]
    app.service.library.addFolder(folderName)

    val firstLevelFolders: List[Folder] = app.service.folder.immediateChildren(repo.rootFolderId)

    halt(200,
      ssp("htmx/folder_list.ssp", "folders" -> firstLevelFolders)
    )
  }

  // This just clears the body of the modal
  get("/close-modal") {
    ssp("htmx/close_modal")
  }
}
