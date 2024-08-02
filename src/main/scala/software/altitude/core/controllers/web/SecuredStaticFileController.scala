package software.altitude.core.controllers.web

import org.scalatra.NotFound
import software.altitude.core.controllers.BaseWebController
import software.altitude.core.models.MimedPreviewData
import software.altitude.core.{Const => C}


class SecuredStaticFileController extends BaseWebController {

  before() {
    requireLogin()
  }

  notFound {
    val pathSegments = request.pathInfo.split("/").toList
    // the first elements (we are not interested in are "", "r")
    require(pathSegments.length == 5, "Content path must include repoId, dataType, and assetId")
    val repoId = pathSegments(2)
    val dataType = pathSegments(3)
    val assetId = pathSegments(4)

    app.service.repository.setContextFromRequest(Some(repoId))
    if (dataType == C.DataStore.PREVIEW) {

      val preview = app.service.fileStore.getPreviewById(assetId)
      contentType = MimedPreviewData.MIME_TYPE
      response.getOutputStream.write(preview.data)
      halt(200)
    }

    if (dataType == C.DataStore.FILE) {
      val data = app.service.fileStore.getAssetById(assetId)
      contentType = data.mimeType
      response.getOutputStream.write(data.data)
      halt(200)
    }

    halt(NotFound("Not Found"))
  }

}
