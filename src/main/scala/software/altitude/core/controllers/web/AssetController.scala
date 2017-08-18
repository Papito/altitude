package software.altitude.core.controllers.web

import software.altitude.core.models.Preview
import software.altitude.core.{Const => C, NotFoundException}
import org.slf4j.LoggerFactory
import software.altitude.core.Const.Api
import software.altitude.core.models.Preview

class AssetController extends BaseWebController {
  private final val log = LoggerFactory.getLogger(getClass)

  get("/:id/preview") {
    val id = params(Api.ID)

    try {
      val preview: Preview = app.service.library.getPreview(id)
      this.contentType = preview.mimeType
      preview.data
    }
    catch {
      case ex: NotFoundException => redirect("/i/1x1.png")
    }
  }

  override def logRequestStart() = Unit
  override def logRequestEnd() = Unit

}
