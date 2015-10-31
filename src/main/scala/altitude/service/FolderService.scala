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

  def hierarchy(rootId: String = C.Folder.Ids.ROOT_PARENT)(implicit txId: TransactionId) = {
    children(DAO.getAll(), parentId = rootId)
  }

  /**
   * Breadcrumbs
   */
  def path(folderId: String)(implicit txId: TransactionId): List[Folder] = {
    val all = getAll()

    val folderEl = all.filter(json => (json \ C.Folder.ID).as[String] == folderId).head
    val folder: Folder = Folder.fromJson(folderEl)
    val parents = findParents(folderId =folderId, all = getAll())

    (folder :: parents).reverse
  }

  def findParents(folderId: String, all: List[JsValue] = List()):  List[Folder] = {
    //FIXME: NotFoundException
    val folderEl = all.filter(json => (json \ C.Folder.ID).as[String] == folderId).head
    val parentId = (folderEl \ C.Folder.PARENT_ID).as[String]
    val parentElements = all filter (json => (json \ C.Folder.ID).as[String] == parentId)

    parentElements.isEmpty match {
      case true => List()
      case false => {
        val folder = Folder.fromJson(parentElements.head)
        List(folder) ++ findParents(folderId = folder.id.get, all)
      }
    }
  }

  /**
   * Get children for the root given, but only a single level - non-recursive
   */
  def immediateChildren(rootId: String = C.Folder.Ids.ROOT_PARENT, all: List[JsValue] = List())
                          (implicit txId: TransactionId) = {
    (all.isEmpty match {
      case true => DAO.getAll()
      case false => all
    }) filter (json => (json \ C.Folder.PARENT_ID).as[String] == rootId)
  }

  private def children(all: List[JsValue], parentId: String)
                         (implicit txId: TransactionId): List[Folder] = {
    val immediateChildren = this.immediateChildren(parentId, all)

    for (folder <- immediateChildren) yield  {
      val id: String = (folder \ C.Folder.ID).as[String]
      val name = (folder \ C.Folder.NAME).as[String]

      Folder(id = Some(id), name = name,
        children = this.children(all, id))
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
      val childrenAndDepths: List[(Int, String)] = flatChildren(DAO.getAll(), id).sortBy(_._1).reverse

      // now delete the children, most-deep first
      childrenAndDepths.foreach{t =>
        val folderId = t._2
        DAO.deleteById(folderId)}

      // return number of folders deleted
      childrenAndDepths.length
    }
  }

  private def flatChildren(all: List[JsValue], parentId: String, depth: Int = 0): List[(Int, String)]  = {
    val childElements = all.filter(j => (j \ C.Folder.PARENT_ID).asOpt[String].contains(parentId))

    // recursively combine with the result of deeper child levels + this one (depth-first)
    (depth, parentId) :: childElements.foldLeft(List[(Int, String)]()) { (res, json) =>
      val folderId = (json \ C.Folder.ID).as[String]
      res ++ flatChildren(all, folderId, depth + 1)}
  }

  override def deleteByQuery(query: Query)(implicit txId: TransactionId = new TransactionId): Int = {
    throw new NotImplementedError("Cannot delete folders by query")
  }
}
