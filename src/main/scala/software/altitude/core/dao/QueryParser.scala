package software.altitude.core.dao

import software.altitude.core.util.Query
import software.altitude.core.{Const => C}

trait QueryParser {
  protected val RESERVED_PARAMS = List(C.Api.Folder.QUERY_ARG_NAME)

  protected def getFolderIds(query: Query): Set[String] = {
    val foldersParam = query.params.filter(_._1 == C.Api.Folder.QUERY_ARG_NAME)


    if (foldersParam.isEmpty) {
      Set[String]()
    }
    else {
      foldersParam.head._2.toString
        .split(s"\\${C.Api.MULTI_VALUE_DELIM}")
        .map(_.trim)
        .filter(_.nonEmpty).toSet
    }
  }

  // parse out system parameters
  protected def getParams(query: Query): Map[String, Any] = query.params.filterKeys(!RESERVED_PARAMS.contains(_))
}
