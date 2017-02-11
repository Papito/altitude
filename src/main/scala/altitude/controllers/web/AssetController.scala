package altitude.controllers.web

import altitude.models.Preview
import altitude.{Const => C, NotFoundException}
import org.slf4j.LoggerFactory

class AssetController extends BaseWebController {
  private final val log = LoggerFactory.getLogger(getClass)

  get("/:id/preview") {
    val id = params(C.Api.ID)

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
