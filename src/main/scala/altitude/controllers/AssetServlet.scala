package altitude.controllers

import altitude.models.{Preview, Asset}

class AssetServlet extends BaseController {
  get("/") {
    val preview: Option[Preview] = this.app.service.library.getPreview("")
    this.contentType = preview.get.mime
  }
}
