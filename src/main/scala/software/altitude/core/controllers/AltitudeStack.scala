package software.altitude.core.controllers

import org.scalatra._
import software.altitude.core.auth.AuthenticationSupport

trait AltitudeStack extends ScalatraServlet with ContentEncodingSupport with AuthenticationSupport
