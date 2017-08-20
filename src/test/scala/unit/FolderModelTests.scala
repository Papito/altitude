package unit

import org.scalatest.FunSuite
import org.scalatest.Matchers._
import software.altitude.core.Util
import software.altitude.core.models.{BaseModel, Folder}

class FolderModelTests extends FunSuite {

  test("uniqueness") {
    val folder1 = new Folder(
      parentId = BaseModel.genId, name = Util.randomStr(30), path = Some(Util.randomStr()))
    val folder2 = new Folder(
      parentId = folder1.parentId, name = folder1.name, path = Some(Util.randomStr()))
    val folder3 = new Folder(
      parentId = BaseModel.genId, name = Util.randomStr(30), path = Some(Util.randomStr()))
    val folder4 = new Folder(
      id = Option(BaseModel.genId), parentId = Util.randomStr(30), name = Util.randomStr(30), path = Some(Util.randomStr()))

    Set(folder1, folder2, folder3, folder4).size shouldEqual 3
  }
}
