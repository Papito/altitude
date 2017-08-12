package altitude.controllers.api

import altitude.Validators.ApiRequestValidator
import altitude.controllers.Utils
import altitude.models.{MetadataField, Asset, Data}
import altitude.{Const => C, NotFoundException}
import org.scalatra.Ok
import org.slf4j.LoggerFactory
import play.api.libs.json.{JsObject, Json}

class AssetController extends BaseApiController {
  private final val log = LoggerFactory.getLogger(getClass)

  get(s"/:${C.Api.ID}") {
    val id = params.get(C.Api.ID).get

    val asset: Asset = app.service.library.getById(id)

    Ok(Json.obj(
      C.Api.Asset.ASSET -> Utils.formatMetadata(app, asset)
    ))
  }

  get(s"/:${C.Api.ID}/data") {
    val id = params.get(C.Api.ID).get

    try {
      val data: Data = app.service.fileStore.getById(id)
      this.contentType = data.mimeType
      data.data
    }
    catch {
      case ex: NotFoundException => redirect("/i/1x1.png")
    }
  }

  get(s"/:${C.Api.ID}/metadata") {
    val id = params.get(C.Api.ID).get
  }

  post(s"/:${C.Api.ID}/move/:${C.Api.Asset.FOLDER_ID}") {
    val id = params.get(C.Api.ID).get
    val folderId = params.get(C.Api.Asset.FOLDER_ID).get
    log.info(s"Moving asset $id to $folderId")

    app.service.library.moveAssetToFolder(id, folderId)

    OK
  }

  post(s"/move/to/:${C.Api.Asset.FOLDER_ID}") {
    val folderId = params.get(C.Api.Asset.FOLDER_ID).get

    log.info(s"Moving assets to $folderId")

    val validator = ApiRequestValidator(List(C.Api.Folder.ASSET_IDS))
    validator.validate(requestJson.get)

    val assetIds = (requestJson.get \ C.Api.Folder.ASSET_IDS).as[Set[String]]

    log.debug(s"Assets to move: $assetIds")

    app.service.library.moveAssetsToFolder(assetIds, folderId)

    OK
  }

  post(s"/:${C.Api.ID}/move/to/triage") {
    val id = params.get(C.Api.ID).get
    log.info(s"Moving $id to TRIAGE")
    app.service.library.moveAssetToTriage(id)

    OK
  }

  post(s"/move/to/triage") {
    log.info(s"Clearing category")

    val validator = ApiRequestValidator(List(C.Api.Folder.ASSET_IDS))
    validator.validate(requestJson.get)

    val assetIds = (requestJson.get \ C.Api.Folder.ASSET_IDS).as[Set[String]]

    log.debug(s"Assets to move to traige: $assetIds")

    app.service.library.moveAssetsToTriage(assetIds)

    OK
  }
}
