package altitude.service

import altitude.Validators.Validator
import altitude.{Const => C, Cleaners, Altitude}
import altitude.dao.FolderDao
import altitude.exceptions.{ValidationException, DuplicateException, NotFoundException}
import altitude.models.Folder
import altitude.models.search.Query
import altitude.transactions.TransactionId

import net.codingwell.scalaguice.InjectorExtensions._
import org.slf4j.LoggerFactory
import play.api.libs.json.{Json, JsObject, JsValue}

object FolderService {
  class NewFolderValidator
    extends Validator(
      required = Some(List(C.Folder.NAME, C.Folder.PARENT_ID)))
}

class FolderService(app: Altitude) extends BaseService[Folder](app){
  private final val log = LoggerFactory.getLogger(getClass)
  override protected val DAO = app.injector.instance[FolderDao]

  override val CLEANER = Some(Cleaners.Cleaner(trim = Some(List(C.Folder.NAME, C.Folder.PARENT_ID))))
  override val VALIDATOR = Some(new FolderService.NewFolderValidator)

  override def add(folder: Folder, queryForDup: Option[Query] = None)
                  (implicit txId: TransactionId = new TransactionId): JsObject = {
    val dupQuery = Query(Map(
      C.Folder.PARENT_ID -> folder.parentId,
      C.Folder.NAME_LC -> folder.nameLowercase))

    try {
      super.add(folder, Some(dupQuery))
    } catch {
      case _: DuplicateException => {
        val ex = ValidationException()
        ex.errors += (C.Folder.NAME -> C.MSG("warn.duplicate"))
        throw ex
      }
    }
  }

  def hierarchy(rootId: String = C.Folder.Ids.ROOT)
               (implicit txId: TransactionId = new TransactionId): List[Folder] = {
    val all = getAll()
    val rootEl = all.find(json => (json \ C.Folder.ID).as[String] == rootId)

    rootId == C.Folder.Ids.ROOT || rootEl.isDefined match {
      case true => children(all, parentId = rootId)
      case false => throw new NotFoundException(s"Cannot get hierarchy. Root folder $rootId does not exist")
    }
  }

  /**
   * Breadcrumbs
   */
  def path(folderId: String)
          (implicit txId: TransactionId = new TransactionId): List[Folder] = {
    // short-circuit for root folder
    if (folderId == C.Folder.Ids.ROOT) {
      return List[Folder]()
    }

    val all = getAll()

    val folderEl = all.find(json => (json \ C.Folder.ID).as[String] == folderId)
    val folder: Folder = folderEl.isDefined match {
      case true => Folder.fromJson(folderEl.get)
      case false => throw new NotFoundException(s"Folder with ID '$folderId' not found")
    }

    val parents = findParents(folderId =folderId, all = getAll())

    (folder :: parents).reverse
  }

  /**
   * Get children for the root given, but only a single level - non-recursive
   */
  def immediateChildren(rootId: String = C.Folder.Ids.ROOT, all: List[JsValue] = List())
                       (implicit txId: TransactionId = new TransactionId): List[Folder] = {
    val _all = all.isEmpty match {
      case true => DAO.getAll()
      case false => all
    }

    val rootEl = _all.find(json => (json \ C.Folder.ID).as[String] == rootId)

    val folders = rootId == C.Folder.Ids.ROOT || rootEl.isDefined match {
      case true =>  _all
        .filter(json => (json \ C.Folder.PARENT_ID).as[String] == rootId)
        .map{json => Folder.fromJson(json)}
      case false => throw new NotFoundException(
        s"Cannot get immediate children. Root folder $rootId does not exist")
    }

    folders.sortBy(_.nameLowercase)
  }

  override def deleteById(id: String)
                         (implicit txId: TransactionId = new TransactionId): Int = {
    txManager.withTransaction[Int] {
      val res: Option[JsObject] = DAO.getById(id)

      val folder: Folder = res.isDefined match {
        case true => res.get
        case false => throw new NotFoundException(s"Cannot find folder ID '$id'")
      }

      log.info(s"Deleting folder $folder")

      // get the list of tuples - (depth, id), with most-deep first
      val childrenAndDepths: List[(Int, String)] = flatChildren(DAO.getAll(), id).sortBy(_._1).reverse
                                                                               /* sort by depth */

      // now delete the children, most-deep first
      childrenAndDepths.foreach{t =>
        val folderId = t._2
        DAO.deleteById(folderId)}

      // return number of folders deleted
      childrenAndDepths.length
    }
  }

  override def deleteByQuery(query: Query)(implicit txId: TransactionId = new TransactionId): Int = {
    throw new NotImplementedError("Cannot delete folders by query")
  }

  private def children(all: List[JsValue], parentId: String)
                         (implicit txId: TransactionId): List[Folder] = {
    val immediateChildren = this.immediateChildren(parentId, all)

    val folders = for (folder <- immediateChildren) yield  {
      val id: String = (folder \ C.Folder.ID).as[String]
      val name = (folder \ C.Folder.NAME).as[String]

      Folder(id = Some(id), name = name,
        children = this.children(all, id))
    }

    folders.sortBy(_.nameLowercase)
  }

  private def findParents(folderId: String, all: List[JsValue] = List()):  List[Folder] = {
    val folderEl = all.find(json => (json \ C.Folder.ID).as[String] == folderId)

    val parentId = folderEl.isDefined match {
      case true => (folderEl.get \ C.Folder.PARENT_ID).as[String]
      case false => throw new NotFoundException(s"Folder with ID '$folderId' not found")
    }

    val parentElements = all filter (json => (json \ C.Folder.ID).as[String] == parentId)

    parentElements.isEmpty match {
      case true => List()
      case false => {
        val folder = Folder.fromJson(parentElements.head)
        List(folder) ++ findParents(folderId = folder.id.get, all)
      }
    }
  }

  private def flatChildren(all: List[JsValue], parentId: String, depth: Int = 0): List[(Int, String)]  = {
    val childElements = all.filter(j => (j \ C.Folder.PARENT_ID).asOpt[String].contains(parentId))

    // recursively combine with the result of deeper child levels + this one (depth-first)
    (depth, parentId) :: childElements.foldLeft(List[(Int, String)]()) { (res, json) =>
      val folderId = (json \ C.Folder.ID).as[String]
      res ++ flatChildren(all, folderId, depth + 1)}
  }

  def move(movedId: String, targetId: String)(implicit txId: TransactionId): Unit = {
    log.debug(s"Moving folder $movedId to $targetId")
    val updateJson = Json.obj(C.Folder.PARENT_ID -> targetId)
    this.update(movedId, updateJson)
  }
}
