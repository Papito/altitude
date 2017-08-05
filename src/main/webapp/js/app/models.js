function mapDictionaryToArray(dictionary) {
  var result = [];
  for (var key in dictionary) {
    if (dictionary.hasOwnProperty(key)) {
      result.push({ key: key, value: dictionary[key] });
    }
  }

  return result;
}
function Asset(data) {
  this.id = data ? data.id : null;
  this.selected = ko.observable(false);
  this.fileName = data ? data.filename : null;
  this.extractedMetadata = data ? ko.observableArray(mapDictionaryToArray(data.extracted_metadata)) : ko.observableArray();
  this.metadata = data ? ko.observableArray(mapDictionaryToArray(data.metadata)) : ko.observableArray();
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