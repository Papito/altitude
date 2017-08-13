package altitude.controllers.api

import altitude.Validators.ApiRequestValidator
import altitude.controllers.Util
import altitude.models.{Metadata, MetadataField, Asset, Data}
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
      C.Api.Asset.ASSET -> Util.withFormattedMetadata(app, asset)
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

  post(s"/:${C.Api.ID}/metadata/:${C.Api.Asset.METADATA_FIELD_ID}") {
    val assetId = params.get(C.Api.ID).get
    val fieldId = params.get(C.Api.Asset.METADATA_FIELD_ID).get
    val newValue = (requestJson.get \ C.Api.Metadata.VALUE).as[String]
    log.info(s"Updating meta field [$fieldId] for asset [$assetId] with new value [$newValue]")

    val currentMetadata = app.service.metadata.getMetadata(assetId)

    val currentValues = currentMetadata.get(fieldId).isEmpty  match {
      case true => Set[String]()
      case false => currentMetadata.get(fieldId).get
    }

    val newValues = currentValues + newValue
    val data = Map[String, Set[String]](fieldId -> newValues)

    app.service.metadata.updateMetadata(assetId, new Metadata(data))

    val newMetadata = app.service.metadata.getMetadata(assetId)

    Ok(Json.obj(
      C.Api.Asset.METADATA -> app.service.metadata.toJson(newMetadata)
    ))
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
