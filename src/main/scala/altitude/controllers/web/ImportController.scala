package altitude.controllers.web


class ImportController extends BaseWebController {
  get("/") {
    contentType = "text/html"
    ssp("import")
  }
}

