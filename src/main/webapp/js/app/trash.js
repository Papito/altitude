TrashViewModel = AssetsViewModel.extend({
  constructor : function() {
    "use strict";

    this.base();
    console.log('Initializing trash view model');

    this.moveSelectedAssetsFromTrashTreeEl = $('#folderSelModal-moveSelectedAssetsFromTrash-tree');
    this.moveSelectedAssetsFromTrashEl = $('#folderSelModal-moveSelectedAssetsFromTrash-actionBtn');
    // when a folder is selected, enable the "move" button
    this.moveSelectedAssetsFromTrashTreeEl.bind(
        "select_node.jstree", function(){
          self.moveSelectedAssetsFromTrashEl.removeAttr('disabled');
        }
    );

    this.moveAssetFromTrashTreeEl = $('#folderSelModal-moveAssetFromTrash-tree');
    this.moveAssetFromTrashEl = $('#folderSelModal-moveAssetFromTrash-actionBtn');
    // when a folder is selected, enable the "move" button
    this.moveAssetFromTrashTreeEl.bind(
        "select_node.jstree", function(){
          self.moveAssetFromTrashEl.removeAttr('disabled');
        }
    );

  },

  setUpRightClickContext: function() {
    var self = this;

    $.contextMenu({
      selector: 'div.result-box',
      items: {
        move: {
          name: "Move",
          callback: function(key, opt){
            var assetId = opt.$trigger.context.attributes.getNamedItem('asset_id').nodeValue;
            self.moveAssetOrSelectedFromTrash(assetId)
          }
        },
        delete: {
          name: "Restore",
          callback: function(key, opt){
            var assetId = opt.$trigger.context.attributes.getNamedItem('asset_id').nodeValue;
            self.restoreAssetOrSelected(assetId)
          }
        }
      }
    });
  },

  moveAssetOrSelectedFromTrash: function(assetId) {
    var self = this;

    if (self.selectedCount()) {
      self.showMoveSelectedAssetsFromTrash();
    }
    else {
      self.showMoveAssetFromTrash(assetId);
    }
  },

  restoreAssetOrSelected: function(assetId) {
    var self = this;

    if (self.selectedCount()) {
      self.restoreSelectedAssets();
    }
    else {
      self.restoreAsset(assetId);
    }
  },

  showMoveSelectedAssetsFromTrash: function() {
    var self = this;

    var successCallback = function() {
      $('#folderSelModal-moveSelectedAssetsFromTrash').modal();
    };


    self._showFolderModal(
        self.moveSelectedAssetsFromTrashTreeEl,
        self.moveSelectedAssetsFromTrashEl,
        successCallback);
  },

  showMoveAssetFromTrash: function() {
    var self = this;

    var successCallback = function() {
      $('#folderSelModal-moveAssetFromTrash').modal();
    };


    self._showFolderModal(
        self.moveAssetFromTrashTreeEl,
        self.moveAssetFromTrashEl,
        successCallback);
  },

  moveSelectedAssetsFromTrash: function() {
    console.log('Moving selected assets from trash');
  },

  moveAssetFromTrash: function(assetId) {
    console.log('Moving asset from trash');
  },

  restoreSelectedAssets: function() {
    console.log('Restoring selected assets');
  },

  restoreAsset: function(assetId) {
    var self = this;

    console.log('Restoring asset');

    var opts = {
      'successCallback': function() {
        self.refreshResults();
        self.blinkSuccess("Asset restored");
      },
      'finally': function() {
        self.actionState = null;
      }
    };

    this.post('/api/v1/trash/' + assetId + '/restore', opts);
  },

  getUrl: function() {
    var self = this;
    return '/api/v1/trash/p/' +  self.currentPage() + '/rpp/' + self.resultsPerPage()
  }

});
