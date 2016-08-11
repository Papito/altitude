package altitude.controllers.web

import altitude.controllers.BaseController
import altitude.models.User

class BaseWebController extends BaseController {
  // FIXME: USER
  private var _user: Option[User] = Some(User(id = Some("1"), rootFolderId = "0", uncatFolderId = "1"))

  implicit def user: User = _user.get

  def user_= (arg: User): Unit = {
    if (_user.isDefined)
      throw new RuntimeException("Cannot set user twice")
    _user = Some(arg)
  }

}
