package software.altitude.core.service

import software.altitude.core.RequestContext

import javax.servlet.http.HttpServletRequest

class UrlService {
  def getUrlForPersonView(request: HttpServletRequest, personId: String): String = {
    s"/r/${RequestContext.getRepository.persistedId}?view=person&personId=$personId" + gerFragment(request)
  }

  private def gerFragment(request: HttpServletRequest): String = {
    val currentUrl = request.getHeader("HX-Current-URL")
    val urlFragment = currentUrl.split("#").lastOption.getOrElse("")
    if (urlFragment.isEmpty) "" else s"#${urlFragment}"
  }
}
