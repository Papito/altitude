package models

class ImportAsset(pathArg: String) {
  require(pathArg != Nil)

  def path = pathArg
}

