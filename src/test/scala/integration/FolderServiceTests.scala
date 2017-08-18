package integration

import software.altitude.core.{ValidationException, NotFoundException, IllegalOperationException}
import software.altitude.core.models.Folder
import org.scalatest.DoNotDiscover
import org.scalatest.Matchers._

@DoNotDiscover class FolderServiceTests (val config: Map[String, Any]) extends IntegrationTestCore {

  test("hierarchy") {
    /*
      folder1
        folder1_1
        folder1_2
      */

    val folder1: Folder = altitude.service.folder.addFolder("folder1")

    folder1.parentId shouldEqual ctx.repo.rootFolderId

    val folder1_1: Folder = altitude.service.folder.addFolder(
      name = "folder1_1", parentId = folder1.id)

        folder1_1.parentId should not be None

        val folder1_2: Folder = altitude.service.folder.addFolder(
          name = "folder1_2", parentId = folder1.id)

        folder1.id should contain(folder1_2.parentId)

        /*
          folder2
            folder2_1
              folder2_1_1
            folder2_2
          */
        val folder2: Folder = altitude.service.folder.addFolder(
          name = "folder2")

        folder2.parentId shouldEqual ctx.repo.rootFolderId

        val folder2_1: Folder = altitude.service.folder.addFolder(
          name = "folder2_1", parentId = folder2.id)

        folder2.id should contain(folder2_1.parentId)

        val folder2_1_1: Folder = altitude.service.folder.addFolder(
          name = "folder2_1_1", parentId = folder2_1.id)

        folder2_1.id should contain(folder2_1_1.parentId)

        val folder2_2: Folder = altitude.service.folder.addFolder(
          name = "folder2_2", parentId = folder2.id)

        folder2.id should contain(folder2_2.parentId)

        val hierarchy = altitude.service.folder.hierarchy()
        hierarchy.length shouldBe 2
        hierarchy.head.children.length shouldBe 2
        hierarchy(1).children.length shouldBe 2
        hierarchy(1).children.head.children.length shouldBe 1
        hierarchy(1).children(1).children.length shouldBe 0

        // check immediate children of the second folder
        val immediateChildren = altitude.app.service.folder.immediateChildren(rootId = folder2_1.id.get)
        immediateChildren.length should be (1)

        // check breadcrumb
        val path = altitude.app.service.folder.pathComponents(folderId = folder2_1_1.id.get)
        path.length shouldBe 4
        path(1).id shouldBe folder2.id
        path.last.id shouldBe folder2_1_1.id

        SET_SECONDARY_REPO()

        val hierarchy2 = altitude.service.folder.hierarchy()
        hierarchy2 shouldBe empty
  }

  test("bad hierarchy root") {
    intercept[NotFoundException] {
      altitude.service.folder.hierarchy(rootId = Some("bogus"))
    }
  }

  test("root path") {
    val path: List[Folder] = altitude.app.service.folder.pathComponents(folderId = ctx.repo.rootFolderId)
    path.length should equal(0)
  }

  test("bad path") {
    intercept[NotFoundException] {
      altitude.app.service.folder.pathComponents(folderId = "bogus")
    }
  }

  test("duplicate") {
    val folder: Folder = altitude.service.folder.addFolder("folder1")

    intercept[ValidationException] {
      altitude.service.folder.addFolder(folder.name)
    }

    val folders = altitude.service.folder.hierarchy()
    folders.length shouldBe 1
  }

  test("validate") {
    intercept[ValidationException] {
      altitude.service.folder.addFolder("")
    }
    intercept[ValidationException] {
      altitude.service.folder.addFolder(" ")
    }
    intercept[ValidationException] {
      altitude.service.folder.addFolder(" ")
    }
    intercept[ValidationException] {
      altitude.service.folder.addFolder("\t \t   ")
    }
  }

  test("sanitize") {
    val folder1: Folder = altitude.service.folder.addFolder(" folder  ")
    folder1.name shouldEqual "folder"

    val folder2:Folder = altitude.service.folder.addFolder(" Folder one \n")
    folder2.name shouldEqual "Folder one"
  }

  test("delete folder") {
    /*
  folder1
    folder1_1
      folder1_1_1
        folder1_1_1_1
        folder1_1_1_2
    folder1_2
  */
    val folder1: Folder = altitude.service.folder.addFolder("folder1")

    val folder1_1: Folder = altitude.service.folder.addFolder(
      name = "folder1_1", parentId = folder1.id)

    val folder1_1_1: Folder = altitude.service.folder.addFolder(
      name = "folder1_1_1", parentId = folder1_1.id)

    val folder1_1_1_1: Folder = altitude.service.folder.addFolder(
      name = "folder1_1_1_1", parentId = folder1_1_1.id)

    val folder1_1_1_2: Folder = altitude.service.folder.addFolder(
      name = "folder1_1_1_2", parentId = folder1_1_1.id)

    val folder1_2: Folder = altitude.service.folder.addFolder(
      name = "folder1_2", parentId = folder1.id)

    altitude.service.library.deleteFolderById(folder1.id.get)

    intercept[NotFoundException] {
      altitude.service.folder.getById(folder1.id.get)
    }

    // children should be removed as well
    intercept[NotFoundException] {
      altitude.service.folder.getById(folder1_1.id.get)
    }
    intercept[NotFoundException] {
      altitude.service.folder.getById(folder1_1_1.id.get)
    }
    intercept[NotFoundException] {
      altitude.service.folder.getById(folder1_1_1_1.id.get)
    }
    intercept[NotFoundException] {
      altitude.service.folder.getById(folder1_1_1_2.id.get)
    }
    intercept[NotFoundException] {
      altitude.service.folder.getById(folder1_2.id.get)
    }
    intercept[NotFoundException] {
      altitude.service.folder.getById(folder1.id.get)
    }
  }

  test("delete bad folder") {
    intercept[NotFoundException] {
      altitude.service.library.deleteFolderById("bogus")
    }
  }

  test("delete root folder") {
    intercept[IllegalOperationException] {
      altitude.service.library.deleteFolderById(ctx.repo.rootFolderId)
    }
  }

  test("delete sys folder") {
    altitude.service.folder.getSystemFolders.foreach { sysFolder =>
      intercept[IllegalOperationException] {
        altitude.service.library.deleteFolderById(sysFolder.id.get)
      }
    }
  }

  test("add a child to a sys folder") {
    altitude.service.folder.getSystemFolders.foreach { sysFolder =>
      intercept[IllegalOperationException] {
        altitude.service.folder.addFolder(name = "folder1", parentId = sysFolder.id)
        }
    }
  }

  test("move folder") {
    /*
  folder1
    folder1_1
      folder1_1_1
    folder1_2
  folder2
  */
    val folder1: Folder = altitude.service.folder.addFolder("folder1")

    val folder1_1: Folder = altitude.service.folder.addFolder(
      name = "folder1_1", parentId = folder1.id)

    val folder1_1_1: Folder = altitude.service.folder.addFolder(
      name = "folder1_1_1", parentId = folder1_1.id)

    val folder1_2: Folder = altitude.service.folder.addFolder(
      name = "folder1_2", parentId = folder1.id)

    val folder2: Folder = altitude.service.folder.addFolder("folder2")

    // assert initial state
    // target
    altitude.app.service.folder.immediateChildren(rootId = folder2.id.get).length should be (0)
    // source
    altitude.app.service.folder.immediateChildren(rootId = folder1_1.id.get).length should be (1)

    // move folder1_1_1 to folder2
    altitude.service.folder.move(folder1_1_1.id.get, folder2.id.get)
    // target
    altitude.app.service.folder.immediateChildren(rootId = folder2.id.get).length should be (1)
    // source
    altitude.app.service.folder.immediateChildren(rootId = folder1_1.id.get).length should be (0)
  }

  test("move folder to root") {
    /*
  folder1
    folder1_1
      folder1_1_1
  folder2
  */
    val folder1: Folder = altitude.service.folder.addFolder("folder1")

    val folder1_1: Folder = altitude.service.folder.addFolder(
      name = "folder1_1", parentId = folder1.id)

    val folder1_1_1: Folder = altitude.service.folder.addFolder(
      name = "folder1_1_1", parentId = folder1_1.id)

    val folder2: Folder = altitude.service.folder.addFolder("folder2")

    // assert initial state
    altitude.app.service.folder.immediateChildren(rootId = ctx.repo.rootFolderId).length should be (2)

    altitude.service.folder.move(folder1_1_1.id.get, ctx.repo.rootFolderId)
    altitude.app.service.folder.immediateChildren(rootId = ctx.repo.rootFolderId).length should be (3)

    altitude.service.folder.move(folder1_1.id.get, ctx.repo.rootFolderId)
    altitude.app.service.folder.immediateChildren(rootId = ctx.repo.rootFolderId).length should be (4)
  }

  test("illegal move") {
    /*
    folder1
      folder1_1
        folder1_1_1
    folder2
        folder1_1_1
    folder3
        FOLDER1_1_1
    */
    val folder1: Folder = altitude.service.folder.addFolder("folder1")

    val folder1_1: Folder = altitude.service.folder.addFolder(
      name = "folder1_1", parentId = folder1.id)

    val folder1_1_1: Folder = altitude.service.folder.addFolder(
      name = "folder1_1_1", parentId = folder1_1.id)

    val folder2: Folder = altitude.service.folder.addFolder("folder2")

    // create folder1_1_1 as a duplicate under a different parent
    altitude.service.folder.addFolder(
      name = "folder1_1_1", parentId = folder2.id)

    val folder3: Folder = altitude.service.folder.addFolder("folder3")

    // create folder1_1_1 as a duplicate under a different parent
    altitude.service.folder.addFolder(
      name = "FOLDER1_1_1", parentId = folder3.id)

    // move into itself
    intercept[IllegalOperationException] {
      altitude.service.folder.move(folder1.id.get, folder1.id.get)
    }

    // move into a child
    intercept[IllegalOperationException] {
      altitude.service.folder.move(folder1.id.get, folder1_1_1.id.get)
    }

    // move into a parent with the same immediate child name
    intercept[ValidationException] {
      altitude.service.folder.move(folder1_1_1.id.get, folder2.id.get)
    }

    // move into a parent with the same immediate child name (different casing_
    intercept[ValidationException] {
      altitude.service.folder.move(folder1_1_1.id.get, folder3.id.get)
    }

    // move into a folder that does not exist
    intercept[ValidationException] {
    altitude.service.folder.move(folder1.id.get, "bogus")
    }
  }

  test("rename") {
    val folder1: Folder = altitude.service.folder.addFolder("folder")

    altitude.service.library.renameFolder(folder1.id.get, "newName")

    val renamedFolder: Folder = altitude.service.folder.getById(folder1.id.get)
    renamedFolder.name shouldEqual "newName"
  }

  test("illegal rename") {
    val folder1: Folder = altitude.service.folder.addFolder("folder")

    intercept[ValidationException] {
      altitude.service.library.renameFolder(folder1.id.get, folder1.name)
    }

    // rename to same but with different casing
    intercept[ValidationException] {
      altitude.service.library.renameFolder(folder1.id.get, folder1.name.toUpperCase)
    }

    // rename a system folder
    intercept[IllegalOperationException] {
      altitude.service.library.renameFolder(ctx.repo.rootFolderId, folder1.name)
    }
  }
}
