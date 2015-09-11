package altitude.controllers.web

import altitude.controllers.BaseController
import altitude.exceptions.NotFoundException
import altitude.models.Preview
import altitude.{Const => C}

class AssetController extends BaseWebController {
  get("/:id/preview") {
    val id = params(C.Api.ID)

    try {
      val preview: Preview = app.service.library.getPreview(id)
      this.contentType = preview.mime_type
      preview.data
    }
    catch {
      case ex: NotFoundException => redirect("/i/1x1.png") //FIXME: preload and return binary data
    }
  }
}
