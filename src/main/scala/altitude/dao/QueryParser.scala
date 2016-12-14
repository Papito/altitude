package altitude.dao

import altitude.models.search.Query
import altitude.{Const => C}

trait QueryParser {
  protected val RESERVED_PARAMS = List(C.Api.Folder.QUERY_ARG_NAME)

  protected def getFolderIds(query: Query) = {
    val foldersParam = query.params.filter(_._1 == C.Api.Folder.QUERY_ARG_NAME)
    foldersParam.isEmpty match {
      case true => Set[String]()
      case false => {
        foldersParam.head._2.toString
          .split(s"\\${C.Api.MULTI_VALUE_DELIM}")
          .map(_.trim)
          .filter(_.nonEmpty).toSet
      }
    }
  }

  // parse out system parameters
  protected def getParams(query: Query) = query.params.filterKeys(!RESERVED_PARAMS.contains(_))
}
