package software.altitude.core.controllers.htmx

import org.scalatra.Route
import software.altitude.core.RequestContext
import software.altitude.core.controllers.BaseHtmxController

/**
  * @ /htmx/album/
 */
class AlbumActionController extends BaseHtmxController{

  before() {
    requireLogin()
  }

  val showAlbumsTab: Route = get("/tab") {
    app.service.repository.setContextFromUserActiveRepo(RequestContext.getAccount)

    ssp("htmx/albums", "albums" -> List())
  }

}
