package software.altitude.core.dao.jdbc

import org.slf4j.LoggerFactory
import play.api.libs.json.JsObject
import play.api.libs.json.Json
import software.altitude.core.AltitudeAppContext
import software.altitude.core.RequestContext
import software.altitude.core.models.Folder
import software.altitude.core.{Const => C}

abstract class FolderDao(val appContext: AltitudeAppContext) extends BaseDao with software.altitude.core.dao.FolderDao {
  private final val log = LoggerFactory.getLogger(getClass)

  override final val tableName = "folder"

  override protected def makeModel(rec: Map[String, AnyRef]): JsObject = {
    val model = Folder(
      id = Some(rec(C.Base.ID).asInstanceOf[String]),
      name = rec(C.Folder.NAME).asInstanceOf[String],
      parentId = rec(C.Folder.PARENT_ID).asInstanceOf[String],
      isRecycled = rec(C.Folder.IS_RECYCLED).asInstanceOf[Int] match {
        case 0 => false
        case 1 => true
      },
      numOfAssets = rec(C.Folder.NUM_OF_ASSETS).asInstanceOf[Int]
    )
    addCoreAttrs(model, rec)
  }

  override def add(jsonIn: JsObject): JsObject = {
    val folder = jsonIn: Folder

    val id = folder.id match {
      case Some(id) => id
      case None => BaseDao.genId
    }

    val sql = s"""
        INSERT INTO $tableName (
                      ${C.Folder.ID}, ${C.Folder.REPO_ID}, ${C.Folder.NAME}, ${C.Folder.NAME_LC}, ${C.Folder.PARENT_ID}
                    )
             VALUES (?, ? , ?, ?, ?)
    """

    val sqlVals: List[Any] = List(
      id,
      RequestContext.getRepository.id.get,
      folder.name,
      folder.nameLowercase,
      folder.parentId)

    addRecord(jsonIn, sql, sqlVals)
    jsonIn ++ Json.obj(C.Base.ID -> id)
  }
}
