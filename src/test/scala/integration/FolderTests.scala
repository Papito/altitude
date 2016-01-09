package integration

import altitude.exceptions.{IllegalOperationException, ValidationException, DuplicateException, NotFoundException}
import altitude.models.Folder
import org.scalatest.DoNotDiscover
import org.scalatest.Matchers._
import integration.util.Text
import altitude.{Const => C}

@DoNotDiscover class FolderTests (val config: Map[String, String]) extends IntegrationTestCore {
  test("hierarchy") {
    /*
      folder1
        folder1_1
        folder1_2
      */
    val folder1: Folder = altitude.service.folder.add(
      Folder(name = "folder1"))

    folder1.parentId should be("0")

    val folder1_1: Folder = altitude.service.folder.add(
      Folder(name = "folder1_1", parentId = folder1.id.get))

    folder1_1.parentId should not be None

    val folder1_2: Folder = altitude.service.folder.add(
      Folder(name = "folder1_2", parentId = folder1.id.get))

    folder1.id should contain(folder1_2.parentId)

    /*
      folder2
        folder2_1
          folder2_1_1
        folder2_2
      */
    val folder2: Folder = altitude.service.folder.add(
      Folder(name = "folder2"))

    folder2.parentId should be("0")

    val folder2_1: Folder = altitude.service.folder.add(
      Folder(name = "folder2_1", parentId = folder2.id.get))

    folder2.id should contain(folder2_1.parentId)

    val folder2_1_1: Folder = altitude.service.folder.add(
      Folder(name = "folder2_1_1", parentId = folder2_1.id.get))

    folder2_1.id should contain(folder2_1_1.parentId)

    val folder2_2: Folder = altitude.service.folder.add(
      Folder(name = "folder2_2", parentId = folder2.id.get))

    folder2.id should contain(folder2_2.parentId)

    val folders = altitude.service.folder.hierarchy()
    folders.length should be(2)
    folders.head.children.length should be (2)
    folders(1).children.length should be (2)
    folders(1).children.head.children.length should be(1)
    folders(1).children(1).children.length should be(0)

    // check immediate children of the second folder
    val immediateChildren = altitude.app.service.folder.immediateChildren(rootId = folder2_1.id.get)
    immediateChildren.length should be (1)

    // check breadcrumb
    val path = altitude.app.service.folder.path(folderId = folder2_1_1.id.get)
    path.length should be(4)
    path(1).id should be(folder2.id)
    path.last.id should be(folder2_1_1.id)
  }

  test("bad hierarchy root") {
    intercept[NotFoundException] {
      altitude.service.folder.hierarchy(rootId = "bogus")
    }
  }

  test("root path") {
    val path: List[Folder] = altitude.app.service.folder.path(folderId = C.Folder.Ids.ROOT)
    path.length should equal(0)
  }

  test("bad path") {
    intercept[NotFoundException] {
      altitude.app.service.folder.path(folderId = "bogus")
    }
  }

  test("duplicate") {
    val folder: Folder = altitude.service.folder.add(Folder(name = "folder1"))

    intercept[ValidationException] {
      altitude.service.folder.add(Folder(name = folder.name))
    }

    val folders = altitude.service.folder.hierarchy()
    folders.length should be(1)
  }

  test("validate") {
    intercept[ValidationException] {
      altitude.service.folder.add(Folder(name = ""))
    }
    intercept[ValidationException] {
      altitude.service.folder.add(Folder(name = " "))
    }
    intercept[ValidationException] {
      altitude.service.folder.add(Folder(name = " "))
    }
    intercept[ValidationException] {
      altitude.service.folder.add(Folder(name = "\t \t   "))
    }
  }

  test("sanitize") {
    val folder1: Folder = altitude.service.folder.add(Folder(name = " folder  "))
    folder1.name should be("folder")

    val folder2:Folder = altitude.service.folder.add(Folder(name = " Folder one \n"))
    folder2.name should be("Folder one")
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
    val folder1: Folder = altitude.service.folder.add(
      Folder(name = "folder1"))

    val folder1_1: Folder = altitude.service.folder.add(
      Folder(name = "folder1_1", parentId = folder1.id.get))

    val folder1_1_1: Folder = altitude.service.folder.add(
      Folder(name = "folder1_1_1", parentId = folder1_1.id.get))

    val folder1_1_1_1: Folder = altitude.service.folder.add(
      Folder(name = "folder1_1_1_1", parentId = folder1_1_1.id.get))

    val folder1_1_1_2: Folder = altitude.service.folder.add(
      Folder(name = "folder1_1_1_2", parentId = folder1_1_1.id.get))

    val folder1_2: Folder = altitude.service.folder.add(
      Folder(name = "folder1_2", parentId = folder1.id.get))

    val deleted = altitude.service.folder.deleteById(folder1.id.get)
    deleted should be(6)

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
  }

  test("delete bad folder") {
    intercept[NotFoundException] {
      altitude.service.folder.deleteById("bogus")
    }
  }

  test("delete root folder") {
    intercept[IllegalOperationException] {
      altitude.service.folder.deleteById(Folder.ROOT.id.get)
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
    val folder1: Folder = altitude.service.folder.add(
      Folder(name = "folder1"))

    val folder1_1: Folder = altitude.service.folder.add(
      Folder(name = "folder1_1", parentId = folder1.id.get))

    val folder1_1_1: Folder = altitude.service.folder.add(
      Folder(name = "folder1_1_1", parentId = folder1_1.id.get))

    val folder1_2: Folder = altitude.service.folder.add(
      Folder(name = "folder1_2", parentId = folder1.id.get))

    val folder2: Folder = altitude.service.folder.add(
      Folder(name = "folder2"))

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
    val folder1: Folder = altitude.service.folder.add(
      Folder(name = "folder1"))

    val folder1_1: Folder = altitude.service.folder.add(
      Folder(name = "folder1_1", parentId = folder1.id.get))

    val folder1_1_1: Folder = altitude.service.folder.add(
      Folder(name = "folder1_1_1", parentId = folder1_1.id.get))

    val folder2: Folder = altitude.service.folder.add(
      Folder(name = "folder2"))

    // assert initial state
    altitude.app.service.folder.immediateChildren(rootId = Folder.ROOT.id.get).length should be (2)

    altitude.service.folder.move(folder1_1_1.id.get, Folder.ROOT.id.get)
    altitude.app.service.folder.immediateChildren(rootId = Folder.ROOT.id.get).length should be (3)

    altitude.service.folder.move(folder1_1.id.get, Folder.ROOT.id.get)
    altitude.app.service.folder.immediateChildren(rootId = Folder.ROOT.id.get).length should be (4)
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
    val folder1: Folder = altitude.service.folder.add(
      Folder(name = "folder1"))

    val folder1_1: Folder = altitude.service.folder.add(
      Folder(name = "folder1_1", parentId = folder1.id.get))

    val folder1_1_1: Folder = altitude.service.folder.add(
      Folder(name = "folder1_1_1", parentId = folder1_1.id.get))

    val folder2: Folder = altitude.service.folder.add(
      Folder(name = "folder2"))

    // create folder1_1_1 as a duplicate under a different parent
    altitude.service.folder.add(
      Folder(name = "folder1_1_1", parentId = folder2.id.get))

    val folder3: Folder = altitude.service.folder.add(
      Folder(name = "folder3"))

    // create folder1_1_1 as a duplicate under a different parent
    altitude.service.folder.add(
      Folder(name = "FOLDER1_1_1", parentId = folder3.id.get))

    // move into itself
    intercept[IllegalOperationException] {
      altitude.service.folder.move(folder1.id.get, folder1.id.get)
    }

    // move into a child
    intercept[IllegalOperationException] {
      altitude.service.folder.move(folder1.id.get, folder1_1_1.id.get)
    }

    // move into a parent with the same immediate child name
    intercept[DuplicateException] {
      altitude.service.folder.move(folder1_1_1.id.get, folder2.id.get)
    }

    // move into a parent with the same immediate child name (different casing_
    intercept[DuplicateException] {
      altitude.service.folder.move(folder1_1_1.id.get, folder3.id.get)
    }

    // move into a folder that does not exist
    intercept[IllegalOperationException] {
    altitude.service.folder.move(folder1.id.get, "bogus")
    }
  }

  test("rename") {
    val folder1: Folder = altitude.service.folder.add(
      Folder(name = "folder"))

    altitude.service.folder.rename(folder1.id.get, "newName")

    val renamedFolder: Folder = altitude.service.folder.getById(folder1.id.get)
    renamedFolder.name should be("newName")
  }

  test("illegal rename") {
    val folder1: Folder = altitude.service.folder.add(
      Folder(name = "folder"))

    intercept[ValidationException] {
      altitude.service.folder.rename(folder1.id.get, folder1.name)
    }

    // rename to same but with different casing
    intercept[ValidationException] {
      altitude.service.folder.rename(folder1.id.get, folder1.name.toUpperCase)
    }

    // rename a system folder
    intercept[IllegalOperationException] {
      altitude.service.folder.rename(Folder.ROOT.id.get, folder1.name)
    }
  }

}
