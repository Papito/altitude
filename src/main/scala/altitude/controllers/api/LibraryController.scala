package altitude.controllers.api

import altitude.{Const => C}


import org.slf4j.LoggerFactory

class LibraryController extends BaseApiController {
  private final val log = LoggerFactory.getLogger(getClass)

  get(s"/assets/move/:id/:${C.Api.Asset.FOLDER_ID}") {
    val id = params.get(C.Api.ID).get
    val folderId = params.get(C.Api.Asset.FOLDER_ID).get
    log.info(s"Moving $id to $folderId")

    app.service.library.moveToFolder(id, folderId)
  }

  get(s"/assets/move/:id/uncategorized") {
    val id = params.get(C.Api.ID).get
    log.info(s"Moving $id to UNCATEGORIZED")
    app.service.library.moveToUncategorized(id)
  }

  get(s"/assets/move/:id/trash") {
    val id = params.get(C.Api.ID).get
    log.info(s"Moving $id to TRASH")
    app.service.library.moveToTrash(id)
  }}
