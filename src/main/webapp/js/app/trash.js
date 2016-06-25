TrashViewModel = AssetsViewModel.extend({
  constructor : function() {
    "use strict";
    var self = this;
    
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

  showMoveAssetFromTrash: function(assetId) {
    var self = this;

    var successCallback = function() {
      self.actionState = assetId;
      $('#folderSelModal-moveAssetFromTrash').modal();
    };


    self._showFolderModal(
        self.moveAssetFromTrashTreeEl,
        self.moveAssetFromTrashEl,
        successCallback);
  },

  moveAssetFromTrash: function() {
    var self = this;

    var assetId = self.actionState;
    var moveToFolderId = self.moveAssetFromTrashTreeEl.jstree('get_selected')[0];
    self.moveAssetToFolder(assetId, moveToFolderId);
  },

  moveAssetToFolder: function(assetId, folderId) {
    var self = this;

    var opts = {
      'successCallback': function() {
        self.refreshResults();
        self.blinkSuccess("Asset moved");
      },
      'finally': function() {
        self.actionState = null;
        $('#folderSelModal-moveAssetFromTrash').modal('hide');
      }
    };

    this.post('/api/v1/trash/' + assetId + '/move/' + folderId, opts);
  },

  moveSelectedAssetsFromTrash: function() {
    var self = this;
    var moveToFolderId = self.moveSelectedAssetsFromTrashTreeEl.jstree('get_selected')[0];
    console.log('move selected assets to ' + moveToFolderId);

    var opts = {
      'data': {
        'asset_ids': self.selectedIds()
      },
      'successCallback': function() {
        self.clearSelection();
        self.refreshResults();
        self.blinkSuccess("Assets moves");
      },
      'finally': function() {
        self.clearSelection();
        $('#folderSelModal-moveSelectedAssetsFromTrash').modal('hide');
      }
    };

    self.post('/api/v1/trash/move/to/' + moveToFolderId, opts);
  },

  restoreSelectedAssets: function() {
    var self = this;

    console.log('Restoring selected assets');

    var assetIds = self.selectedIds();

    var opts = {
      'data': {
        'asset_ids': assetIds
      },
      'successCallback': function() {
        self.refreshResults();
        self.blinkSuccess("Assets restored");
        self.clearSelection();
      },
      'finally': function() {
        self.clearSelection();
      }
    };

    self.post('/api/v1/trash/restore', opts);
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
