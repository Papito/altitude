package software.altitude.core.dao.jdbc

trait Stats { this: BaseDao =>
  override protected def selectColumns: List[String] = List("*")
}
