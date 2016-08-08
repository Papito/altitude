package unit

import altitude.Util
import altitude.models.Folder
import org.scalatest.FunSuite
import org.scalatest.Matchers._

class FolderModelTests extends FunSuite {

  test("uniqueness") {
    val userId = Util.randomStr()

    val folder1 = new Folder(
      userId = userId, parentId = Util.randomStr(30), name = Util.randomStr(30))
    val folder2 = new Folder(
      userId = userId, parentId = folder1.parentId, name = folder1.name)
    val folder3 = new Folder(
      userId = userId, parentId = Util.randomStr(30), name = Util.randomStr(30))
    val folder4 = new Folder(
      userId = userId, id = Option(Util.randomStr(30)), parentId = Util.randomStr(30), name = Util.randomStr(30))

    Set(folder1, folder2, folder3, folder4).size should be(3)
  }
}
