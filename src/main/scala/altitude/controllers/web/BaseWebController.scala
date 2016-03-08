package altitude.controllers.web

import altitude.controllers.BaseController
import com.github.mustachejava.DefaultMustacheFactory

object BaseWebController {
  val mustacheFactory = new DefaultMustacheFactory()
}

class BaseWebController extends BaseController {

}
