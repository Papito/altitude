package altitude.controllers.api

import altitude.Validators.ApiValidator
import altitude.models.Folder
import altitude.{Const => C}
import org.scalatra.Ok
import org.slf4j.LoggerFactory
import play.api.libs.json.{JsObject, Json}

class FolderController extends BaseApiController {
  private final val log = LoggerFactory.getLogger(getClass)

  override val HTTP_POST_VALIDATOR = Some(ApiValidator(List(
    C.Api.Folder.NAME, C.Api.Folder.PARENT_ID
  )))

  get() {
    val folders = app.service.folder.hierarchy()

    Ok(Json.obj(
      C.Api.Folder.HIERARCHY -> folders.map(_.toJson)
    ))
  }

  get("/:id") {
    val id = params.get(C.Api.ID).get
    val folder: Folder = app.service.folder.getById(id)

    Ok(Json.obj(
      C.Api.Folder.FOLDER -> folder.toJson
    ))
  }

  get(s"/:${C.Api.Folder.PARENT_ID}/children") {
    val parentId = params.getAs[String](C.Api.Folder.PARENT_ID).get
    val all = app.service.folder.getAll
    val folders = app.service.folder.immediateChildren(parentId, all = all)
    val sysFolders = app.service.folder.getSysFolders(all = all)
    val path = app.service.folder.path(parentId)

    Ok(Json.obj(
      C.Api.Folder.PATH -> path.map(_.toJson),
      C.Api.Folder.FOLDERS -> folders.map(_.toJson),
      C.Api.Folder.SYSTEM -> JsObject(
        sysFolders.map { case (folderId, folder) =>
          folderId -> folder.toJson
        }.toSeq
      )
    ))
  }

  post("/") {
    val name = (requestJson.get \ C.Api.Folder.NAME).as[String]
    val parentId = (requestJson.get \ C.Api.Folder.PARENT_ID).as[String]

    val newFolder: Folder = app.service.folder.addFolder(name = name, parentId = Some(parentId))
    log.debug(s"New folder: $newFolder")

    Ok(Json.obj(C.Api.Folder.FOLDER -> newFolder.toJson))
  }

  delete("/:id") {
    val id = params.get(C.Api.ID).get
    log.info(s"Deleting folder $id")
    app.service.folder.deleteById(id)

    OK
  }

  put("/:id") {
    val id = params.get(C.Api.ID).get
    val newName = (requestJson.get \ C.Api.Folder.NAME).asOpt[String]
    val newParentId = (requestJson.get \ C.Api.Folder.PARENT_ID).asOpt[String]

    if (newName.isDefined) {
      app.service.folder.rename(id, newName.get)
    }

    if (newParentId.isDefined) {
      app.service.folder.move(id, newParentId.get)
    }

    OK
  }
}
