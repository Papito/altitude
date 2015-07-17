package altitude.controllers

import altitude.models.Preview

class AssetServlet extends BaseController {
  get("/:id/preview") {
    val id = params("id")
    val preview: Preview = this.app.service.library.getPreview(id).get
    this.contentType = preview.mime_type
    preview.data
  }
}
