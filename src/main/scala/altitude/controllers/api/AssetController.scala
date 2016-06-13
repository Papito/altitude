package altitude.controllers.api

import altitude.Validators.{ApiValidator, Validator}
import altitude.exceptions.ValidationException
import altitude.{Const => C}
import org.scalatra.{Ok, ResponseStatus, BadRequest}
import org.slf4j.LoggerFactory
import play.api.libs.json.{JsObject, Json}

class AssetController extends BaseApiController {
  private final val log = LoggerFactory.getLogger(getClass)

  post(s"/:id/move/:${C("Api.Asset.FOLDER_ID")}") {
    val id = params.get(C("Api.ID")).get
    val folderId = params.get(C("Api.Asset.FOLDER_ID")).get
    log.info(s"Moving $id to $folderId")

    app.service.library.moveAssetToFolder(id, folderId)

    OK
  }

  post(s"/move/to/:${C("Api.Asset.FOLDER_ID")}") {
    val folderId = params.get(C("Api.Asset.FOLDER_ID")).get

    log.info(s"Moving assets to $folderId")

    val validator = ApiValidator(List(C("Api.Folder.ASSET_IDS")))
    validator.validate(requestJson.get)

    val assetIds = (requestJson.get \ C("Api.Folder.ASSET_IDS")).as[Set[String]]

    log.debug(s"Assets to move: $assetIds")

    app.service.library.moveAssetsToFolder(assetIds, folderId)

    OK
  }

  post(s"/:id/move/to/uncategorized") {
    val id = params.get(C("Api.ID")).get
    log.info(s"Moving $id to UNCATEGORIZED")
    app.service.library.moveAssetToUncategorized(id)

    OK
  }


  post(s"/move/to/uncategorized") {
    log.info(s"Clearing category")

    val validator = ApiValidator(List(C("Api.Folder.ASSET_IDS")))
    validator.validate(requestJson.get)

    val assetIds = (requestJson.get \ C("Api.Folder.ASSET_IDS")).as[Set[String]]

    log.debug(s"Assets to move to uncategorized: $assetIds")

    app.service.library.moveAssetsToUncategorized(assetIds)

    OK
  }
}
