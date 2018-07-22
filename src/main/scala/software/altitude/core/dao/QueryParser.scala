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

  /*
   - Filters out system parameters
   - Replaces booleans with integers, as this is what's used in databases
    */
  protected def getParams(query: Query): Map[String, Any] = {
    query.params.filterKeys(!RESERVED_PARAMS.contains(_)).map { v: (String, Any) =>
      v._2 match {
        case x: String => (v._1, x)
        case x: Boolean => if (x) (v._1, 1) else (v._1, 0)
      }
    }
  }
}
