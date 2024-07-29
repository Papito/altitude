package software.altitude.core.controllers

import org.scalatra.scalate.ScalateSupport
import software.altitude.core.Const
import software.altitude.core.Environment
import software.altitude.core.RequestContext
import software.altitude.core.models.Repository
import software.altitude.core.models.User
import software.altitude.core.util.Query

class BaseWebController extends BaseController with ScalateSupport
