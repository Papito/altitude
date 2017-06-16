function Asset(data) {
  this.id = data ? data.id : null;
  this.selected = ko.observable(false);
  this.fileName = data ? data.filename : null;
}

function Folder(data) {
  this.id = data ? data.id : null;
  this.name = data ? data.name : null;
  this.numOfAssets = data ? data.num_of_assets : 0;
  this.depth = data ? data.depth : 0;
}