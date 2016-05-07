package altitude.controllers.api

import altitude.Validators.{ApiValidator, Validator}
import altitude.exceptions.ValidationException
import altitude.{Const => C}
import org.scalatra.{ResponseStatus, BadRequest}
import org.slf4j.LoggerFactory
import play.api.libs.json.{JsObject, Json}

class AssetController extends BaseApiController {
  private final val log = LoggerFactory.getLogger(getClass)

  post(s"/:id/move/to/folder/:${C("Api.Asset.FOLDER_ID")}") {
    val id = params.get(C("Api.ID")).get
    val folderId = params.get(C("Api.Asset.FOLDER_ID")).get
    log.info(s"Moving $id to $folderId")

    app.service.library.moveAssetToFolder(id, folderId)
  }

  post(s"/move/to/:${C("Api.Asset.FOLDER_ID")}") {
    val folderId = params.get(C("Api.Asset.FOLDER_ID")).get
    log.info(s"Moving assets to $folderId")

    if (request.body.isEmpty) {
      throw ValidationException(C("msg.err.empty_request_body"))
    }

    val json: JsObject = Json.parse(request.body).as[JsObject]

    val validator = ApiValidator(List(C("Api.Folder.ASSET_IDS")))
    validator.validate(json)

    val assetIds = (json \ C("Api.Folder.ASSET_IDS")).as[Set[String]]

    log.debug(s"Assets to move $assetIds")

    app.service.library.moveAssetsToFolder(assetIds, folderId)
  }

  post(s"/:id/move/to/uncategorized") {
    val id = params.get(C("Api.ID")).get
    log.info(s"Moving $id to UNCATEGORIZED")
    app.service.library.moveToUncategorized(id)
  }

  post(s"/:id/move/to/trash") {
    val id = params.get(C("Api.ID")).get
    log.info(s"Moving $id to TRASH")
    app.service.library.moveToTrash(id)
  }}
