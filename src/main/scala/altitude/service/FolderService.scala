package altitude.service

import altitude.Validators.Validator
import altitude.dao.FolderDao
import altitude.exceptions.{DuplicateException, IllegalOperationException, NotFoundException, ValidationException}
import altitude.models.search.Query
import altitude.models.{Folder, User}
import altitude.{Altitude, Cleaners, Const => C, Context}
import net.codingwell.scalaguice.InjectorExtensions._
import org.slf4j.LoggerFactory
import play.api.libs.json._

object FolderService {
  class FolderValidator
    extends Validator(
      required = Some(List(C.Folder.NAME, C.Folder.PARENT_ID)))
}

class FolderService(app: Altitude) extends BaseService[Folder](app) {
  private final val log = LoggerFactory.getLogger(getClass)
  override protected val DAO = app.injector.instance[FolderDao]

  override val CLEANER = Some(Cleaners.Cleaner(
    trim = Some(
      List(C.Folder.NAME, C.Folder.PARENT_ID))))

  override val VALIDATOR = Some(
    new FolderService.FolderValidator)

  override def add(folder: Folder, queryForDup: Option[Query] = None)
                  (implicit ctx: Context): JsObject = {

    if (isSystemFolder(Some(folder.parentId))) {
      throw new IllegalOperationException("Cannot add a child to a system folder")
    }

    val dupQuery = Query(ctx.user, Map(
      C.Folder.PARENT_ID -> folder.parentId,
      C.Folder.NAME_LC -> folder.nameLowercase))

    try {
      super.add(folder, Some(dupQuery))
    } catch {
      case _: DuplicateException => {
        val ex = ValidationException()
        ex.errors += (C.Folder.NAME -> C.Msg.Warn.DUPLICATE)
        throw ex
      }
    }
  }

  def addFolder(name: String, parentId: Option[String] = None)
                  (implicit ctx: Context): JsObject = {

    val folder = Folder(
      userId = ctx.user.id.get,
      name = name,
      parentId = if (parentId.isDefined) parentId.get else ctx.user.rootFolderId)

    add(folder)
  }

  /**
   * Return ALL folders - system and non-system
   */
  override def getAll(implicit ctx: Context): List[JsObject] = {
    txManager.asReadOnly[List[JsObject]] {
      addAssetCount(DAO.getAll)
    }
  }

  def getUserRootFolder()(implicit ctx: Context) = Folder(
    id = Some(ctx.user.rootFolderId),
    userId = ctx.user.id.get,
    parentId = ctx.user.rootFolderId,
    name = C.Folder.Names.ROOT
  )

  def getUserUncatFolder()(implicit ctx: Context) = Folder(
    id = Some(ctx.user.uncatFolderId),
    userId = ctx.user.id.get,
    parentId = ctx.user.rootFolderId,
    name = C.Folder.Names.UNCATEGORIZED
  )

  def getUserSystemFolders()(implicit ctx: Context): List[Folder] =
    List(getUserUncatFolder())

  def isRootFolder(id: Option[String])(implicit ctx: Context) =
    id.contains(ctx.user.rootFolderId)

  def isSystemFolder(id: Option[String])(implicit ctx: Context) =
    getUserSystemFolders().exists(_.id == id)


  private def addAssetCount(folders: List[JsObject])
                           (implicit ctx: Context): List[JsObject] = {
    folders.map{ json =>
      val id = (json \ C.Base.ID).as[String]
      val assetCount = flatChildren(id, folders).toSeq.map(_.numOfAssets).sum

      json ++ JsObject(Seq(
        C.Folder.NUM_OF_ASSETS -> JsNumber(assetCount)))
    }
  }

  /**
   * Return all NON-system folders.
   */
  def getNonSysFolders(all: List[JsObject] = List())
                      (implicit ctx: Context): List[JsObject] = {
    txManager.asReadOnly[List[JsObject]] {
      val _all = if (all.isEmpty) getAll else all
      _all.filter(json => {
        val id = (json \ C.Base.ID).asOpt[String]
        !isSystemFolder(id)
      })
    }
  }

  /**
   * Get the entire hierarchy, with nested children. The root folders are returned
   * as a list.
   */
  def hierarchy(rootId: Option[String] = None, all: List[JsObject] = List())
               (implicit ctx: Context): List[Folder] = {
    val _rootId = if (rootId.isDefined) rootId.get else ctx.user.rootFolderId

    txManager.asReadOnly {
      val nonSysFolders = if (all.isEmpty) getNonSysFolders() else getNonSysFolders(all)

      val rootEl = nonSysFolders.find(json => (json \ C.Base.ID).as[String] == _rootId)

      isRootFolder(Some(_rootId)) || rootEl.isDefined match {
        case true => children(_rootId, nonSysFolders)
        case false => throw NotFoundException(s"Cannot get hierarchy. Root folder $rootId does not exist")
      }
    }
  }

  /**
   * Breadcrumbs.
   * This is the parent list, only reversed.
   */
  def path(folderId: String)
          (implicit ctx: Context): List[Folder] = {
    // short-circuit for root folder
    if (isRootFolder(Some(folderId))) {
      return List[Folder]()
    }

    txManager.asReadOnly[List[Folder]] {
      val nonSysFolders = getNonSysFolders()

      val folderEl = nonSysFolders.find(json => (json \ C.Base.ID).as[String] == folderId)
      val folder: Folder = folderEl.isDefined match {
        case true => Folder.fromJson(folderEl.get)
        case false => throw NotFoundException(s"Folder with ID '$folderId' not found")
      }

      val parents = findParents(folderId =folderId, all = nonSysFolders)

      List(getUserRootFolder()) ::: (folder :: parents).reverse
    }
  }

  /**
   * Get children for the root given, but only a single level - non-recursive
   */
  def immediateChildren(rootId: String, all: List[JsObject] = List())
                       (implicit ctx: Context): List[Folder] = {

    txManager.asReadOnly[List[Folder]] {
      val nonSysFolders = if (all.isEmpty) getNonSysFolders() else getNonSysFolders(all)

      nonSysFolders.filter(json => {
          val id = (json \ C.Base.ID).as[String]
          val parentId = (json \ C.Folder.PARENT_ID).as[String]
          parentId == rootId && !isSystemFolder(Some(id))
        })
        .map{json => Folder.fromJson(json)}
        .sortBy(_.nameLowercase)
    }
  }

  override def deleteById(id: String)
                         (implicit ctx: Context): Int = {
    if (isRootFolder(Some(id))) {
      throw new IllegalOperationException("Cannot delete the root folder")
    }

    if (isSystemFolder(Some(id))) {
      throw new IllegalOperationException("Cannot delete system folder")
    }

    txManager.withTransaction[Int] {
      val res: Option[JsObject] = DAO.getById(id)

      val folder: Folder = res.isDefined match {
        case true => res.get
        case false => throw NotFoundException(s"Cannot find folder ID '$id'")
      }

      log.info(s"Deleting folder $folder")

      // get the list of tuples - (depth, id), with most-deep first
      val childrenAndDepths: List[(Int, String)] = flatChildrenIdsWithDepths(id, getNonSysFolders()).sortBy(_._1).reverse
                                                                                                    /* sort by depth */

      // now delete the children, most-deep first
      txManager.withTransaction[Int] {
        childrenAndDepths.foreach{t =>
          val folderId = t._2
          DAO.deleteById(folderId)}

        // return number of folders deleted
        childrenAndDepths.length
      }
    }
  }

  override def deleteByQuery(query: Query)
                            (implicit ctx: Context): Int = {
    throw new NotImplementedError("Cannot delete folders by query")
  }

  /**
   * Returns a nested list of children for a given folder.
   * Not a flat list! Subsequent children are in each folders' "children" element.
    */
  private def children(parentId: String, all: List[JsObject])
                      (implicit ctx: Context): List[Folder] = {
    val nonSysFolders = if (all.isEmpty) getNonSysFolders() else getNonSysFolders(all)
    val immediateChildren = this.immediateChildren(parentId, nonSysFolders)

    val folders = for (folder <- immediateChildren) yield  {
      val id: String = (folder \ C.Base.ID).as[String]
      val name = (folder \ C.Folder.NAME).as[String]
      val assetCount = (folder \ C.Folder.NUM_OF_ASSETS).as[Int]

      Folder(
        id = Some(id),
        userId = ctx.user.id.get,
        name = name,
        parentId = parentId,
        children = this.children(id, nonSysFolders),
        numOfAssets = assetCount)
    }

    folders.sortBy(_.nameLowercase)
  }

  /**
   * Returns a list of parents for a given folder ID.
   * The list goes from closest parent to farthest.
   */
  private def findParents(folderId: String, all: List[JsObject])
                         (implicit ctx: Context):  List[Folder] = {
    val nonSysFolders = if (all.isEmpty) getNonSysFolders() else getNonSysFolders(all)
    val folderEl = nonSysFolders.find(json => (json \ C.Base.ID).as[String] == folderId)

    val parentId = folderEl.isDefined match {
      case true => (folderEl.get \ C.Folder.PARENT_ID).as[String]
      case false => throw NotFoundException(s"Folder with ID '$folderId' not found")
    }

    val parentElements = nonSysFolders filter (json => (json \ C.Base.ID).as[String] == parentId)

    parentElements.isEmpty match {
      case true => List()
      case false => {
        val folder = Folder.fromJson(parentElements.head)
        List(folder) ++ findParents(folderId = folder.id.get, nonSysFolders)
      }
    }
  }

  override def getById(id: String)
                      (implicit ctx: Context): JsObject = {
    if (isRootFolder(Some(id))) getUserRootFolder() else super.getById(id)
  }

  def getByIdWithChildAssetCounts(id: String, all: List[JsObject] = List())
                                 (implicit ctx: Context): JsObject = {
    val nonSysFolders = if (all.isEmpty) getNonSysFolders() else getNonSysFolders(all)
    val matching = nonSysFolders.filter(j => (j \ C.Base.ID).asOpt[String].contains(id))

    if (matching.isEmpty) {
      throw NotFoundException(s"Base.ID $id not found")
    }

    matching.head
  }

  /**
   * Returns the children for a folder as a flat list.
   */
  def flatChildrenIdsWithDepths(parentId: String, all: List[JsObject] = List(), depth: Int = 0)
                     (implicit ctx: Context): List[(Int, String)] = {
    val nonSysFolders = if (all.isEmpty) getNonSysFolders() else getNonSysFolders(all)

    val childElements = nonSysFolders.filter(j => (j \ C.Folder.PARENT_ID).asOpt[String].contains(parentId))

    // recursively combine with the result of deeper child levels + this one (depth-first)
    (depth, parentId) :: childElements.foldLeft(List[(Int, String)]()) { (res, json) =>
      val folderId = (json \ C.Base.ID).as[String]
      res ++ flatChildrenIdsWithDepths(folderId, nonSysFolders, depth + 1)}
  }

  /**
   * Returns a unique set of folder ids for one OR more folder ids.
   * The difference between the other method signature is that folder depths are not returned.
   * It's a "raw" list of folder ids.
   */
  def flatChildrenIds(parentIds: Set[String], all: List[JsObject] = List())
                     (implicit ctx: Context): Set[String]  =
    parentIds.foldLeft(Set[String]()) {(s, id) => {
      s ++ app.service.folder.flatChildrenIdsWithDepths(parentId = id, all = all).map(_._2).toSet
    }}

  /**
   * Returns flat children as a set of Folder objects
   */
  def flatChildren(parentId: String, all: List[JsObject], depth: Int = 0)
                  (implicit ctx: Context): Set[Folder]  = {
    val parentElements = all filter (json => (json \ C.Base.ID).as[String] == parentId)

    if (parentElements.isEmpty) {
      return Set()
    }

    val parentElement: Folder = parentElements.head
    val childElements = all.filter(j => (j \ C.Folder.PARENT_ID).asOpt[String].contains(parentId))

    // recursively combine with the result of deeper child levels + this one (depth-first)
    (parentElement :: childElements.foldLeft(List[Folder]()) {(res, json) =>
      val folder: Folder = json
      res ++ flatChildren(folder.id.get, all, depth + 1)}).toSet
  }

  def move(folderBeingMovedId: String, destFolderId: String)
          (implicit ctx: Context): Unit = {

    if (isRootFolder(Some(folderBeingMovedId))) {
      throw new IllegalOperationException("Cannot move the root folder")
    }

    log.debug(s"Moving folder $folderBeingMovedId to $destFolderId")

    // cannot move into itself
    if (folderBeingMovedId == destFolderId) {
      throw IllegalOperationException(s"Cannot move a folder into itself. ID: $folderBeingMovedId")
    }

    txManager.withTransaction {
      // cannot move into own child
      if (flatChildrenIdsWithDepths(folderBeingMovedId, getNonSysFolders()).map(_._2).contains(destFolderId)) {
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
      val dupQuery = Query(ctx.user, Map(
        C.Folder.PARENT_ID -> destFolderId,
        C.Folder.NAME_LC -> folderBeingMoved.nameLowercase))

      val folderForUpdate = Folder(
        userId = ctx.user.id.get,
        parentId = destFolderId,
        name = folderBeingMoved.name)

      try {
        updateById(folderBeingMovedId, folderForUpdate, List(C.Folder.PARENT_ID), Some(dupQuery))
      } catch {
        case ex: DuplicateException => throw ValidationException(
          s"Cannot move to '${destFolder.get.name}' as a folder by this name already exists")
      }
    }
  }

  def rename(folderId: String, newName: String)
            (implicit ctx: Context): Unit = {
    if (isRootFolder(Some(folderId))) {
      throw new IllegalOperationException("Cannot rename the root folder")
    }

    txManager.withTransaction {
      val folder: Folder = getById(folderId)

      // new folder name cannot match the new one
      val dupQuery = Query(ctx.user, Map(
        C.Folder.PARENT_ID -> folder.parentId,
        C.Folder.NAME_LC -> newName.toLowerCase))

      try {
        val folderForUpdate = Folder(
          userId = ctx.user.id.get,
          parentId = folder.parentId,
          name = newName)

        updateById(folderId, folderForUpdate, List(C.Folder.NAME, C.Folder.NAME_LC), Some(dupQuery))
      } catch {
        case _: DuplicateException => {
          val ex = ValidationException()
          ex.errors += C.Folder.NAME -> C.Msg.Warn.DUPLICATE
          throw ex
        }
      }
    }
  }

  def incrAssetCount(folderId: String, count: Int = 1)
                    (implicit ctx: Context) = {
    log.debug(s"Incrementing folder $folderId count by $count")

    txManager.withTransaction {
      DAO.increment(folderId, C.Folder.NUM_OF_ASSETS, count)
    }
  }

  def decrAssetCount(folderId: String, count: Int = 1)
                    (implicit ctx: Context) = {
    log.debug(s"Decrementing folder $folderId count by $count")

    txManager.withTransaction {
      DAO.decrement(folderId, C.Folder.NUM_OF_ASSETS, count)
    }
  }

  def getSysFolders(all: List[JsObject] = List())
                   (implicit ctx: Context): Map[String, Folder] = {
    txManager.asReadOnly[Map[String, Folder]] {
      all.isEmpty match {
        case true => {
          val sysFolderIds: Set[String] = getUserSystemFolders().map(_.id.get).toSet
          val allSysFolders = DAO.getByIds(sysFolderIds)
          getSystemFolderLookup(allSysFolders)
        }
        case false => {
          val allSysFolders = all.filter(json => {
            val id = (json \ C.Base.ID).asOpt[String]
              isSystemFolder(id)
          })
          getSystemFolderLookup(allSysFolders)
        }
      }
    }
  }

  /**
   * Given a list of system folders, create a lookup-by-folder-id map
   */
  private def getSystemFolderLookup(allSysFolders: List[JsObject]): Map[String, Folder] = {
    // create the lookup map
    allSysFolders.map(j => {
      val id = (j \ C.Base.ID).as[String]
      val folder = Folder.fromJson(j)
      id -> folder
    }).toMap
  }
}
