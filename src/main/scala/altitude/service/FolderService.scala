package altitude.service

import altitude.Validators.Validator
import altitude.{Const => C, Cleaners, Altitude}
import altitude.dao.FolderDao
import altitude.exceptions.{IllegalOperationException, ValidationException, DuplicateException, NotFoundException}
import altitude.models.Folder
import altitude.models.search.Query
import altitude.transactions.TransactionId

import net.codingwell.scalaguice.InjectorExtensions._
import org.slf4j.LoggerFactory
import play.api.libs.json.{Json, JsObject, JsValue}

object FolderService {

  class FolderValidator
    extends Validator(
      required = Some(List(C.Folder.NAME, C.Folder.PARENT_ID)))
}

class FolderService(app: Altitude) extends BaseService[Folder](app){
  private final val log = LoggerFactory.getLogger(getClass)
  override protected val DAO = app.injector.instance[FolderDao]

  override val CLEANER = Some(Cleaners.Cleaner(
    trim = Some(
      List(C.Folder.NAME, C.Folder.PARENT_ID))))

  override val VALIDATOR = Some(
    new FolderService.FolderValidator)

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

  /**
   * Get the entire hierarchy, with nested children. The root folders are returned
   * as a list.
   */
  def hierarchy(rootId: String = Folder.ROOT.id.get)
               (implicit txId: TransactionId = new TransactionId): List[Folder] = {
    val all = getAll()
    val rootEl = all.find(json => (json \ C.Folder.ID).as[String] == rootId)

    Folder.IS_ROOT(Some(rootId)) || rootEl.isDefined match {
      case true => children(all, parentId = rootId)
      case false => throw NotFoundException(s"Cannot get hierarchy. Root folder $rootId does not exist")
    }
  }

  /**
   * Breadcrumbs.
   * This is the parent list, only reversed.
   */
  def path(folderId: String)
          (implicit txId: TransactionId = new TransactionId): List[Folder] = {

    // short-circuit for root folder
    if (Folder.IS_ROOT(Some(folderId))) {
      return List[Folder]()
    }

    val all = getAll()

    val folderEl = all.find(json => (json \ C.Folder.ID).as[String] == folderId)
    val folder: Folder = folderEl.isDefined match {
      case true => Folder.fromJson(folderEl.get)
      case false => throw NotFoundException(s"Folder with ID '$folderId' not found")
    }

    val parents = findParents(folderId =folderId, all = getAll())

    List(Folder.ROOT) ::: (folder :: parents).reverse
  }

  /**
   * Get children for the root given, but only a single level - non-recursive
   */
  def immediateChildren(rootId: String = Folder.ROOT.id.get, all: List[JsValue] = List())
                       (implicit txId: TransactionId = new TransactionId): List[Folder] = {
    val _all = all.isEmpty match {
      case true =>  {
        val query = Query(Map(C.Folder.PARENT_ID -> rootId))
        DAO.query(q = query)
      }
      case false => all
    }

    _all
        .filter(json => (json \ C.Folder.PARENT_ID).as[String] == rootId)
        .map{json => Folder.fromJson(json)}
        .sortBy(_.nameLowercase)
  }

  override def deleteById(id: String)(implicit txId: TransactionId = new TransactionId): Int = {
    if (Folder.IS_ROOT(Some(id))) {
      throw new IllegalOperationException("Cannot delete the root folder")
    }

    txManager.withTransaction[Int] {
      val res: Option[JsObject] = DAO.getById(id)

      val folder: Folder = res.isDefined match {
        case true => res.get
        case false => throw NotFoundException(s"Cannot find folder ID '$id'")
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

  /**
   * Returns a nested list of children for a given folder.
   * Not a flat list! Subsequent children are in each folders' "children" element.
    */
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

  /**
   * Returns a list of parents for a given folder ID.
   * The list goes from closest parent to farthest.
   */
  private def findParents(folderId: String, all: List[JsValue] = List()):  List[Folder] = {
    val folderEl = all.find(json => (json \ C.Folder.ID).as[String] == folderId)

    val parentId = folderEl.isDefined match {
      case true => (folderEl.get \ C.Folder.PARENT_ID).as[String]
      case false => throw NotFoundException(s"Folder with ID '$folderId' not found")
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

  /**
   * Handle special case of the root folder, which does not persist
   */
  override def getById(id: String)(implicit txId: TransactionId = new TransactionId): JsObject = {
    Folder.IS_ROOT(Some(id)) match {
      case true => Folder.ROOT
      case false => super.getById(id)
    }
  }

  /**
   * Returns the children for a folder as a flat list.
   * Useful for quick searches and verifying that a certain child is in a folder's hierarchy.
   */
  private def flatChildren(all: List[JsValue], parentId: String, depth: Int = 0): List[(Int, String)]  = {
    val childElements = all.filter(j => (j \ C.Folder.PARENT_ID).asOpt[String].contains(parentId))

    // recursively combine with the result of deeper child levels + this one (depth-first)
    (depth, parentId) :: childElements.foldLeft(List[(Int, String)]()) { (res, json) =>
      val folderId = (json \ C.Folder.ID).as[String]
      res ++ flatChildren(all, folderId, depth + 1)}
  }

  def move(folderBeingMovedId: String, destFolderId: String)
          (implicit txId: TransactionId = new TransactionId): Unit = {

    if (Folder.IS_ROOT(Some(folderBeingMovedId))) {
      throw new IllegalOperationException("Cannot move the root folder")
    }

    log.debug(s"Moving folder $folderBeingMovedId to $destFolderId")

    // cannot move into itself
    if (folderBeingMovedId == destFolderId) {
      throw IllegalOperationException(s"Cannot move a folder into itself. ID: $folderBeingMovedId")
    }

    // cannot move into own child
    if (flatChildren(getAll(), folderBeingMovedId).map (_._2).contains(destFolderId)) {
      throw IllegalOperationException(s"Cannot move a folder into own child. $destFolderId ID in $folderBeingMovedId path")
    }

    var destFolder: Option[Folder] = None
    // check that the destination folder exists
    try {
      destFolder = Some(getById(destFolderId))
    } catch {
      case ex: NotFoundException => throw ValidationException(s"Destination folder ID $destFolderId does not exist")
    }

    val folderBeingMoved: Folder = getById(folderBeingMovedId)

    // destination parent cannot have folder by the same name
    val dupQuery = Query(Map(
      C.Folder.PARENT_ID -> destFolderId,
      C.Folder.NAME_LC -> folderBeingMoved.nameLowercase))

    val folderForUpdate = Folder(parentId = destFolderId, name = folderBeingMoved.name)

    try {
      updateById(folderBeingMovedId, folderForUpdate, List(C.Folder.PARENT_ID), Some(dupQuery))
    } catch {
      case ex: DuplicateException => throw ValidationException(
        s"Cannot move to '${destFolder.get.name}' as a folder by this name already exists")
    }
  }

  def rename(folderId: String, newName: String)
            (implicit txId: TransactionId = new TransactionId): Unit = {
    if (Folder.IS_ROOT(Some(folderId))) {
      throw new IllegalOperationException("Cannot rename the root folder")
    }

    val folder: Folder = getById(folderId)

    // new folder name cannot match the new one
    val dupQuery = Query(Map(
      C.Folder.PARENT_ID -> folder.parentId,
      C.Folder.NAME_LC -> newName.toLowerCase))

    try {
      val folderForUpdate = Folder(parentId = folder.parentId, name = newName)
      updateById(folderId, folderForUpdate, List(C.Folder.NAME, C.Folder.NAME_LC), Some(dupQuery))
    } catch {
      case _: DuplicateException => {
        val ex = ValidationException()
        ex.errors += (C.Folder.NAME -> C.MSG("warn.duplicate"))
        throw ex
      }
    }
  }
}
