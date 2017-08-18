package software.altitude.core.controllers.api

import software.altitude.core.Validators.ApiRequestValidator
import software.altitude.core.controllers.Util
import software.altitude.core.models.Asset
import software.altitude.core.util.Query
import software.altitude.core.{Const => C}
import org.scalatra.Ok
import org.slf4j.LoggerFactory
import play.api.libs.json.Json
import software.altitude.core.Const
import software.altitude.core.Const.Api
import software.altitude.core.Const.Api.{Search, Trash, Asset, Folder}

class TrashController extends BaseApiController {
  private final val log = LoggerFactory.getLogger(getClass)

  post(s"/recycle/:id") {
    val id = params.get(Api.ID).get
    log.info(s"Moving $id to TRASH")
    app.service.library.recycleAsset(id)

    OK
  }

  post(s"/recycle") {
    log.info(s"Deleting assets")

    val validator = ApiRequestValidator(List(Folder.ASSET_IDS))
    validator.validate(requestJson.get)

    val assetIds = (requestJson.get \ Api.Folder.ASSET_IDS).as[Set[String]]
    log.debug(s"Assets to move to trash: $assetIds")

    app.service.library.recycleAssets(assetIds)

    OK
  }

  post(s"/:id/move/:${C.Asset.FOLDER_ID}") {
    val id = params.get(Const.Api.ID).get
    val folderId = params.get(Api.Asset.FOLDER_ID).get
    log.info(s"Moving recycled asset $id to $folderId")

    app.service.library.moveAssetToFolder(id, folderId)

    OK
  }

  post(s"/move/to/:${Api.Asset.FOLDER_ID}") {
    val folderId = params.get(Api.Asset.FOLDER_ID).get
    log.info(s"Moving recycled assets to $folderId")

    val validator = ApiRequestValidator(List(Trash.ASSET_IDS))
    validator.validate(requestJson.get)

    val assetIds = (requestJson.get \ Api.Trash.ASSET_IDS).as[Set[String]]
    log.debug(s"Recycled assets to move: $assetIds")

    app.service.library.moveAssetsToFolder(assetIds, folderId)

    OK
  }

  post(s"/:id/restore") {
    val assetId = params.get("id").get
    log.info(s"Restoring asset $assetId")

    app.service.library.restoreRecycledAsset(assetId)

    OK
  }

  post(s"/restore") {
    log.info("Restoring multiple assets")

    val validator = ApiRequestValidator(List(Api.Trash.ASSET_IDS))
    validator.validate(requestJson.get)

    val assetIds = (requestJson.get \ Api.Trash.ASSET_IDS).as[Set[String]]
    log.debug(s"Recycled assets to restore: $assetIds")

    app.service.library.restoreRecycledAssets(assetIds)

    OK
  }

  get(s"/p/:${Search.PAGE}/rpp/:${Api.Search.RESULTS_PER_PAGE}") {
    val rpp = params.getOrElse(Api.Search.RESULTS_PER_PAGE, C.DEFAULT_RPP).toInt
    val page = params.getOrElse(C.Api.Search.PAGE, "1").toInt

    val q = Query(rpp = rpp, page = page)

    val results = app.service.library.queryRecycled(q)

    Ok(Json.obj(
      C.Api.Search.ASSETS -> results.records.map { x =>
        val asset = x: Asset
        Util.withFormattedMetadata(app, asset)
      },
      C.Api.TOTAL_RECORDS -> results.total,
      C.Api.CURRENT_PAGE -> q.page,
      C.Api.TOTAL_PAGES -> results.totalPages,
      C.Api.RESULTS_PER_PAGE -> q.rpp
    ))
  }
}
