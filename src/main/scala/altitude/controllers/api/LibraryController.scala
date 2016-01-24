package altitude.controllers.api

import altitude.{Const => C}


import org.slf4j.LoggerFactory

class LibraryController extends BaseApiController {
  private final val log = LoggerFactory.getLogger(getClass)

  get("/assets/move/:id/:folderId") {
    val id = params.get(C.Api.ID).get
    val folderId = params.get(C.Api.Asset.FOLDER_ID).get
    log.info(s"Moving $id to $folderId")

    app.service.library.moveToFolder(id, folderId)
  }
}
