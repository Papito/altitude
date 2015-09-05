package altitude.controllers

import altitude.exceptions.NotFoundException
import altitude.models.Preview
import org.scalatra.Ok

class SearchController extends BaseController {
  get("/") {
    Ok("{}")
  }
}
