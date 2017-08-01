function Asset(data) {
  this.id = data ? data.id : null;
  this.selected = ko.observable(false);
  this.fileName = data ? data.filename : null;
  this.extractedMetadata = data ? data.extracted_metadata : ko.observable();
  this.metadata = data ? data.metadata : ko.observable();
}

function Folder(data) {
  this.id = data ? data.id : null;
  this.name = data ? data.name : null;
  this.numOfAssets = data ? data.num_of_assets : 0;
  this.depth = data ? data.depth : 0;
  this.path = ko.observableArray();
  this.children = ko.observableArray();
  this.active = ko.observable(false);
}