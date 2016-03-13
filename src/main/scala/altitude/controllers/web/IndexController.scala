package altitude.controllers.web

class IndexController extends BaseWebController {

  get("/") {
    redirect("/client/index")
  }
}
