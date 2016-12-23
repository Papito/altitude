package altitude.dao.mongo

import altitude.dao.QueryParser
import altitude.models.search.Query
import altitude.{Const => C, Context}
import com.mongodb.casbah.Imports._
import org.slf4j.LoggerFactory

class MongoQueryBuilder(collection: MongoCollection) extends QueryParser {
  private final val log = LoggerFactory.getLogger(getClass)

  def toSelectCursor(query: Query)(implicit ctx: Context): MongoCursor = {
    val folderIds: Set[String] = getFolderIds(query)

    // filter out system parameters
    val params = getParams(query)

    val builder = MongoDBObject.newBuilder
    builder ++= params

    builder += (C.Base.REPO_ID -> ctx.repo.id.get)

    if (folderIds.nonEmpty) {
      builder += ("folder_id" -> MongoDBObject("$in" -> folderIds))
    }

    val mongoQuery: DBObject = builder.result()

    val cursor: MongoCursor = query.rpp match {
      case 0 => collection.find(mongoQuery)
      case _ => collection.find(mongoQuery).skip((query.page - 1) * query.rpp).limit(query.rpp)
    }

    log.debug(s"MONGO QUERY: ${cursor.query}")
    cursor
  }
}
