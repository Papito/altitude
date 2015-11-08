package altitude.controllers.api

import altitude.Validators.ApiValidator
import altitude.models.Folder
import org.scalatra.Ok
import org.slf4j.LoggerFactory
import altitude.{Const => C}
import play.api.libs.json.Json

class FolderController  extends BaseApiController {
  private final val log = LoggerFactory.getLogger(getClass)

  override val HTTP_POST_VALIDATOR = Some(ApiValidator(List(
    C.Api.Folder.NAME, C.Api.Folder.PARENT_ID
  )))

  get("/:parentId") {
    val parentId = params.getAs[String](C.Api.Folder.PARENT_ID).get
    val folders = app.service.folder.immediateChildren(parentId)
    Ok(Json.obj(C.Api.Folder.FOLDERS -> folders.map(_.toJson)))
  }

  post("/") {
    val name = params.get(C.Api.Folder.NAME)
    val parentId = params.get(C.Api.Folder.PARENT_ID)

    log.debug(s"Adding new folder '$name' to parent '$parentId'")

    val newFolder: Folder = app.service.folder.add(Folder(name = name.get, parentId = parentId.get))

    log.debug(s"New folder: $newFolder")

    Ok(Json.obj(C.Api.Folder.FOLDER -> newFolder.toJson))
  }
}
