TrashViewModel = AssetsViewModel.extend({
  constructor : function() {
    "use strict";
    var self = this;

    this.base();
    console.log('Initializing trash view model');
  },

  setupFolderNav: function() {
    var self = this;
    self.folders = ko.observableArray();
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

  showAssetDetailModal: function(view, asset) {
    console.log('Trash asset view not implemented');
  },

  moveAssetOrSelectedFromTrash: function(assetId) {
    var self = this;

    if (self.selectedCount()) {
      self.showMoveSelectedAssetsFromTrashModal();
    }
    else {
      self.showMoveAssetFromTrashModal(assetId);
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

  showMoveSelectedAssetsFromTrashModal: function() {
    var self = this;

    var successCallback = function() {
      $('#moveSelectedAssetsFromTrashModal').modal();
    };

    var moveSelectedAssetsFromTrashTreeEl = $('#moveSelectedAssetsFromTrashModal\\.tree');
    var moveSelectedAssetsFromTrashEl = $('#moveSelectedAssetsFromTrashModal\\.actionBtn');

    // when a folder is selected, enable the "move" button
    moveSelectedAssetsFromTrashTreeEl.off("select_node.jstree");
    moveSelectedAssetsFromTrashTreeEl.on(
        "select_node.jstree", function(){
          moveSelectedAssetsFromTrashEl.removeAttr('disabled');
        }
    );

    self.showFolderModal({
      treeEl: moveSelectedAssetsFromTrashTreeEl,
      actionEl: moveSelectedAssetsFromTrashEl,
      successFn: successCallback,
      showRoot: false
    });
  },

  showMoveAssetFromTrashModal: function(assetId) {
    var self = this;

    var successCallback = function() {
      self.actionState = assetId;
      $('#moveAssetFromTrashModal').modal();
    };

    var moveAssetFromTrashTreeEl = $('#moveAssetFromTrashModal\\.tree');
    var moveAssetFromTrashEl = $('#moveAssetFromTrashModal\\.actionBtn');

    // when a folder is selected, enable the "move" button
    moveAssetFromTrashTreeEl.off("select_node.jstree");
    moveAssetFromTrashTreeEl.on(
        "select_node.jstree", function(){
          moveAssetFromTrashEl.removeAttr('disabled');
        }
    );

    self.showFolderModal({
      treeEl: moveAssetFromTrashTreeEl,
      actionEl: moveAssetFromTrashEl,
      successFn: successCallback,
      showRoot: false
    });
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
        self.success("Asset moved");
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
        self.success("Assets moves");
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
        self.success("Assets restored");
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
        self.success("Asset restored");
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
