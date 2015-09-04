package altitude.controllers

import altitude.exceptions.NotFoundException
import altitude.models.Preview

class AssetServlet extends BaseController {
  get("/:id/preview") {
    val id = params("id")

    try {
      val preview: Preview = this.app.service.library.getPreview(id)
      this.contentType = preview.mime_type
      preview.data
    }
    catch {
      case ex: NotFoundException => redirect("/i/1x1.png")
    }
  }
}
