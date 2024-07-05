package software.altitude.core.controllers.htmx

import software.altitude.core.controllers.BaseHtmxController

class UserInterfaceHtmxController extends BaseHtmxController{

  get("/modals/add-folder") {
    val minWidth: Int= params.get("minWidth") match {
      case Some(minWidth) => minWidth.toInt
      case None => 0
    }

    ssp("htmx/add_folder_modal",
      "minWidth" ->minWidth,
      "title" -> "Add Folder")
  }

  post("/folder/add") {
    halt(200, "LOL")
  }

  // This just clears the body of the modal
  get("/close-modal") {
    ssp("htmx/close_modal")
  }
}
