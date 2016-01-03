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

  get() {
    val folders = app.service.folder.hierarchy()
    Ok(Json.obj(
      C.Api.Folder.HIERARCHY ->folders.map(_.toJson)
    ))
  }

  get("/:id") {
    val id = params.get(C.Api.ID).get
    val folder: Folder = app.service.folder.getById(id)

    Ok(Json.obj(
      C.Api.Folder.FOLDER -> folder.toJson
    ))
  }

  get("/:parentId") {
    val parentId = params.getAs[String](C.Api.Folder.PARENT_ID).get
    val folders = app.service.folder.immediateChildren(parentId)

    val path = app.service.folder.path(parentId)

    Ok(Json.obj(
      C.Api.Folder.FOLDERS -> folders.map(_.toJson),
      C.Api.Folder.PATH -> path.map(_.toJson)
    ))
  }

  post("/") {
    val name = params.get(C.Api.Folder.NAME)
    val parentId = params.get(C.Api.Folder.PARENT_ID)

    val newFolder: Folder = app.service.folder.add(Folder(name = name.get, parentId = parentId.get))
    log.debug(s"New folder: $newFolder")

    Ok(Json.obj(C.Api.Folder.FOLDER -> newFolder.toJson))
  }

  delete("/:id") {
    val id = params.get(C.Api.ID)
    log.info(s"Deleting folder $id")
    app.service.folder.deleteById(id.get)
    Ok()
  }

  put("/:id") {
    val id = params.get(C.Api.ID).get
    val newName = params.get(C.Api.Folder.NAME)
    val newParentId = params.get(C.Api.Folder.PARENT_ID)

    if (newName.isDefined) {
      app.service.folder.rename(id, newName.get)
    }

    if (newParentId.isDefined) {
      app.service.folder.move(id, newParentId.get)
    }

    Ok()
  }
}
