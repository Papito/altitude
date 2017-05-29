package altitude.controllers.web

class IndexController extends BaseWebController {

  override def setUser() = Unit
  override def logRequestStart() = Unit
  override def logRequestEnd() = Unit

  get("/") {
    redirect("/client/index.dart")
  }
}
