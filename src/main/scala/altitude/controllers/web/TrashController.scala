package altitude.controllers.web

class TrashController extends BaseWebController {
  get("/") {
    contentType = "text/html"
    ssp("trash")
  }
}
