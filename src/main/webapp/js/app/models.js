function Asset(data) {
  this.id = data ? data.id : null;
  this.path = data ? data.path : null;
  this.fileName = data ? data.filename : null;
  this.createdAt = data ? data.created_at : null;
  this.updatedAt = data ? data.updated_at : null;
}

function Folder(data) {
  this.id = data ? data.id : null;
  this.name = data ? data.name : null;
  this.numOfAssets = data ? data.num_of_assets : null;
}