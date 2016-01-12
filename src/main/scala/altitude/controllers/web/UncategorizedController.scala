package altitude.controllers.web

class UncategorizedController extends BaseWebController {
  get("/") {
    contentType = "text/html"
    ssp("uncategorized")
  }
}
