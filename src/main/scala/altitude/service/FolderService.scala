package altitude.service

import altitude.Altitude
import altitude.dao.FolderDao
import altitude.exceptions.NotFoundException
import altitude.models.Folder
import altitude.models.search.Query
import altitude.transactions.TransactionId

import net.codingwell.scalaguice.InjectorExtensions._
import org.slf4j.LoggerFactory
import play.api.libs.json.{JsObject, JsValue}
import altitude.{Const => C}


class FolderService(app: Altitude) extends BaseService[Folder](app){
  private final val log = LoggerFactory.getLogger(getClass)
  override protected val DAO = app.injector.instance[FolderDao]

  def add(folder: Folder)(implicit txId: TransactionId): JsObject = {
    val dupQuery = Query(Map(
      C.Folder.PARENT_ID -> folder.parentId,
      C.Folder.NAME_LC -> folder.nameLowercase))

    super.add(folder, Some(dupQuery))
  }

  def getHierarchy(rootId: String = C.Folder.Ids.ROOT_PARENT)(implicit txId: TransactionId) = {
    getChildren(DAO.getAll(), parentId = rootId)
  }

  /** Get children for the root given, but only a single level - non-recursive
   */
  def getImmediateChildren(rootId: String = C.Folder.Ids.ROOT_PARENT, all: List[JsValue] = List())
                          (implicit txId: TransactionId) = {
    (all.isEmpty match {
      case true => DAO.getAll()
      case false => all
    }) filter (json => (json \ C.Folder.PARENT_ID).as[String] == rootId)
  }

  private def getChildren(all: List[JsValue], parentId: String)
                         (implicit txId: TransactionId): List[Folder] = {
    val children = getImmediateChildren(parentId, all)

    for (folder <- children) yield  {
      val id: String = (folder \ C.Folder.ID).as[String]
      val name = (folder \ C.Folder.NAME).as[String]

      Folder(id = Some(id), name = name,
        children = getChildren(all, id))
    }
  }

  override def deleteById(id: String)(implicit txId: TransactionId = new TransactionId): Int = {
    txManager.withTransaction[Int] {
      val res: Option[JsObject] = DAO.getById(id)

      val folder: Folder = res.isDefined match {
        case true => res.get
        case false => throw new NotFoundException(C.IdType.ID, id)
      }

      log.info(s"Deleting folder $folder")

      // get the list of tuples - (depth, id), with most-deep first
      val childrenAndDepths: List[(Int, String)] = findChildrenAndFlatten(DAO.getAll(), id).sortBy(_._1).reverse

      // now delete the children, most-deep first
      childrenAndDepths.foreach{t =>
        val folderId = t._2
        DAO.deleteById(folderId)}

      // return number of folders deleted
      childrenAndDepths.size
    }
  }

  private def findChildrenAndFlatten(all: List[JsValue], parentId: String, depth: Int = 0): List[(Int, String)]  = {
    val children = all.filter(j => (j \ C.Folder.PARENT_ID).asOpt[String].contains(parentId))

    // recursively combine with the result of deeper child levels + this one (depth-first)
    (depth, parentId) :: children.foldLeft(List[(Int, String)]()) { (res, json) =>
      val folderId = (json \ C.Folder.ID).as[String]
      res ++ findChildrenAndFlatten(all, folderId, depth + 1)}
  }

  override def deleteByQuery(query: Query)(implicit txId: TransactionId = new TransactionId): Int = {
    throw new NotImplementedError("Cannot delete folders by query")
  }
}
