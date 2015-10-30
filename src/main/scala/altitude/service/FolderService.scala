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

  def getHierarchy(rootId: Option[String] = None)(implicit txId: TransactionId) = findChildren(DAO.getAll())

  private def findChildren(all: List[JsValue], parentId: Option[String] = None): List[Folder] = {
    val children = all.filter(json => (json \ C.Folder.PARENT_ID).asOpt[String] == parentId)

    for (folder <- children) yield  {
      val id: Option[String] = (folder \ C.Folder.ID).asOpt[String]
      val name =  (folder \ C.Folder.NAME).as[String]

      Folder(id = id, name = name,
        children = findChildren(all, id))
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

      // get the list of tuples - (depth, id), with higher depths first
      val childrenAndDepths: List[(Int, String)] = (
        /* add the parent */ (0, id) :: findChildrenAndFlatten(DAO.getAll(), id)
        ).sortBy(_._1).reverse

      // now delete the children, most-depth first

      childrenAndDepths.foreach{t => DAO.deleteById(t._2)}
      childrenAndDepths.size
    }
  }

  private def findChildrenAndFlatten(all: List[JsValue], parentId: String, depth: Int = 1): List[(Int, String)]  = {
    val children = all.filter(j => (j \ C.Folder.PARENT_ID).asOpt[String].contains(parentId))

    // get the list of depths and ids for THIS depth level
    children.foldLeft(List[(Int, String)]()) { (res, json) =>
      val folderId = (json \ C.Folder.ID).as[String]
      (depth, folderId) :: res} ++
    // recursively combine with the result of deeper child levels
    children.foldLeft(List[(Int, String)]()) { (res, json) =>
      val folderId = (json \ C.Folder.ID).as[String]
      res ++ findChildrenAndFlatten(all, folderId, depth + 1)}
  }

  override def deleteByQuery(query: Query)(implicit txId: TransactionId = new TransactionId): Int = {
    throw new NotImplementedError("Cannot delete folders by query")
  }
}
