AssetsViewModel = BaseViewModel.extend({
  constructor : function() {
    "use strict";

    var self = this;

    this.base();
    console.log('Initializing search view model');

    this.searchResults = ko.observableArray();
    this.resultsPerPage = ko.observable(15);
    this.currentPage = ko.observable(1);
    this.totalPages = ko.observable(0);
    this.totalRecords = ko.observable(0);

    this.folders = ko.observableArray();
    this.currentFolderPath = ko.observableArray();
    this.currentFolderId = ko.observable("b10000000000000000000000");
    this._showAddFolder = ko.observable(false);

    this.detailAsset = ko.observable();

    this.stats = {};
    this.stats.totalAssets = ko.observable(0);
    this.stats.triageAssets = ko.observable(0);
    this.stats.recycledAssets = ko.observable(0);

    this.selectedAssetsMap = {};
    this.selectedIds = ko.observableArray();

    this.directoryNames = ko.observableArray();
    this.currentPath = ko.observable();
    this.osDirSeparator = ko.observable();
    this.disableDoubleClick = false;


    this.selectedCount = ko.computed(function() {
      return this.selectedIds().length;
    }, this);

    /*
    Temporary storage for anything that should be acted upon,
    for example, after a modal action is confirmed.

    This is a state variable to avoid passing data around between UI elements,
    and as such should be treated carefully.
     */
    this.actionState = null;

    this.queryString = this.queryString || '';
    console.log('Q = ', this.queryString);

    this.prevPageVisible = ko.computed(function() {
      return this.currentPage() > 1;
    }, this);

    this.nextPageVisible = ko.computed(function() {
      return this.currentPage() < this.totalPages();
    }, this);

    this.detailAssetDataUrl = ko.computed(function() {
      return self.detailAsset() ? '/api/v1/assets/' + self.detailAsset().id + '/data' : null;
    }, this);

    this.detailAssetSelected = ko.computed(function() {
      return self.detailAsset() ? self.detailAsset().selected() : false;
    }, this);

    $('#renameFolderForm').on('submit', function(e) {
      self.resetAllMessages();
      e.preventDefault();
      self.renameFolder();
    });

    $('#addFolderForm').on('submit', function(e) {
      self.resetAllMessages();
      e.preventDefault();
      self.addFolder();
    });

    $('#renameFolderModal').on('shown.bs.modal', function () {
      self.resetAllMessages();
      $('#renameFolderInput').focus().select();
    });

    // register the folder context menu
    $.contextMenu({
      selector: 'li.folder',
      items: {
        rename: {
          name: "Rename",
          callback: function(key, opt){
            self.resetAllMessages();
            var folderId = opt.$trigger.context.attributes.getNamedItem('folder_id').nodeValue;
            self.showRenameFolder(folderId);
          }
        },
        move: {
          name: "Move",
          callback: function(key, opt){
            self.resetAllMessages();
            var folderId = opt.$trigger.context.attributes.getNamedItem('folder_id').nodeValue;
            self.showMoveFolder(folderId);
          }
        },
        delete: {
          name: "Delete",
          callback: function(key, opt){
            var folderId = opt.$trigger.context.attributes.getNamedItem('folder_id').nodeValue;
            self.deleteFolder(folderId);
          }
        }
      }
    });

    self.setUpRightClickContext();

    // initialize commonly used elements
    this.moveToFolderTreeEl = $('#folderSelModal-moveFolder-tree');
    this.moveFolderEl = $('#folderSelModal-moveFolder-actionBtn');
    // when a folder is selected, enable the "move" button
    this.moveToFolderTreeEl.bind(
        "select_node.jstree", function(){
          self.moveFolderEl.removeAttr('disabled');
        }
    );

    this.moveSelectedAssetsToFolderTreeEl = $('#folderSelModal-moveSelectedAssets-tree');
    this.moveSelectedAssetsEl = $('#folderSelModal-moveSelectedAssets-actionBtn');
    // when a folder is selected, enable the "move" button
    this.moveSelectedAssetsToFolderTreeEl.bind(
        "select_node.jstree", function(){
          self.moveSelectedAssetsEl.removeAttr('disabled');
        }
    );

    this.moveAssetToFolderTreeEl = $('#folderSelModal-moveAsset-tree');
    this.moveAssetEl = $('#folderSelModal-moveAsset-actionBtn');
    // when a folder is selected, enable the "move" button
    this.moveAssetToFolderTreeEl.bind(
        "select_node.jstree", function(){
          self.moveAssetEl.removeAttr('disabled');
        }
    );

    this.triageEl = $('#triage');
    this.trashEl = $('#trash');

    /*
     system folders
     */

    // triage
    this.triageEl.droppable({
      accept: ".result-box",
      hoverClass: "highlight",
      tolerance: "pointer"
    });

    this.triageEl.on("drop", function( event, ui ) {
      self.resetAllMessages();
      var assetId = $(ui.draggable.context).attr('asset_id');
      self.moveToTriage(assetId);
    });

    // trash
    this.trashEl.droppable({
      accept: ".result-box",
      hoverClass: "highlight",
      tolerance: "pointer"
    });

    this.trashEl.on("drop", function( event, ui ) {
      self.resetAllMessages();
      var assetId = $(ui.draggable.context).attr('asset_id');
      self.moveToTrash(assetId);
    });

    // when asset modal is closed
    $('#assetModal').on('hidden.bs.modal', function () {
      self.detailAsset(null);
      self.setupResultsHotkeys();
    });

    // get the data
    this.search();
    this.loadFolders(self.currentFolderId());
  },

  setupResultsHotkeys: function() {
    var self = this;

    Mousetrap.reset();

    Mousetrap.bind(['.', 'pagedown'], function() {
      self.resetAllMessages();
      self.gotoNextPage();
    }, 'keyup');

    Mousetrap.bind([',', 'pageup'], function() {
      self.resetAllMessages();
      self.gotoPrevPage();
    }, 'keyup');

    Mousetrap.bind('right', function() {
      self.resetAllMessages();
      self.focusRight();
    }, 'keyup');

    Mousetrap.bind('left', function() {
      self.resetAllMessages();
      self.focusLeft();
    }, 'keyup');

    Mousetrap.bind('up', function() {
      self.resetAllMessages();
      self.focusUp();
    }, 'keyup');

    Mousetrap.bind('down', function() {
      self.resetAllMessages();
      self.focusDown();
    }, 'keyup');

    Mousetrap.bind('shift+right', function() {
      self.resetAllMessages();
      self.selectRight();
    });

    Mousetrap.bind('shift+left', function() {
      self.resetAllMessages();
      self.selectLeft();
    });

    Mousetrap.bind('shift+up', function() {
      self.resetAllMessages();
      self.selectUp();
    });

    Mousetrap.bind('shift+down', function() {
      self.resetAllMessages();
      self.selectDown();
    });

    Mousetrap.bind('return', function() {
      var asset = self.getFocusedAsset();
      self.showAssetDetail(self, asset);
    });

    Mousetrap.bind('esc', function() {
      self.resetAllMessages();
    });

    Mousetrap.bind('s', function() {
      self.resetAllMessages();
      self.selectFocused();
    });

    Mousetrap.bind('d', function() {
      self.resetAllMessages();
      self.deselectFocused();
    });

    Mousetrap.bind('shift+s', function() {
      self.resetAllMessages();
      self.selectAllOnPage();
    });

    Mousetrap.bind('shift+d', function() {
      self.resetAllMessages();
      self.deselectAllOnPage();
    });

    Mousetrap.bind('del', function() {
      self.resetAllMessages();

      // if there is a selection, show the confirmation dialog
      if (self.selectedCount()) {
        self.showMoveSelectedToTrash();
        return;
      }

      // else delete currently focused
      var focusedAsset = self.getFocusedAsset();

      if (!focusedAsset) {
        self.warning('No assets focused');
        return;
      }
      self.moveToTrash(focusedAsset.id);
    });

  },

  setupAssetDetailHotkeys: function() {
    var self = this;

    Mousetrap.reset();

    Mousetrap.bind(['.', 'pagedown'], function() {
      self.resetAllMessages();
      self.gotoNextAssetDetail();
    }, 'keyup');

    Mousetrap.bind([',', 'pageup'], function() {
      self.resetAllMessages();
      self.gotoPrevAssetDetail();
    }, 'keyup');

    Mousetrap.bind('right', function() {
      self.resetAllMessages();
      self.gotoNextAssetDetail();
    }, 'keyup');

    Mousetrap.bind('left', function() {
      self.resetAllMessages();
      self.gotoPrevAssetDetail();
    }, 'keyup');

    Mousetrap.bind('up', function() {
      self.resetAllMessages();
      self.gotoPrevAssetDetail();
    }, 'keyup');

    Mousetrap.bind('down', function() {
      self.resetAllMessages();
      self.gotoNextAssetDetail();
    }, 'keyup');

    Mousetrap.bind('s', function() {
      self.resetAllMessages();
      self.selectFocused();
      self.detailAsset().selected(true);
    });

    Mousetrap.bind('d', function() {
      self.resetAllMessages();
      self.deselectFocused();
      self.detailAsset().selected(false);
    });

    Mousetrap.bind('del', function() {
      self.resetAllMessages();

      // after we delete the asset, display the one focused in its place
      var cb = function() {
        self.showAssetDetail(self, self.getFocusedAsset());
      };

      self.moveToTrash(self.detailAsset().id, cb);

    });
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
            self.moveAssetOrSelected(assetId)
          }
        },
        delete: {
          name: "Move to Trash",
          callback: function(key, opt){
            var assetId = opt.$trigger.context.attributes.getNamedItem('asset_id').nodeValue;
            self.moveAssetOrSelectedToTrash(assetId)
          }
        }
      }
    });
  },

  moveAssetOrSelected: function(assetId) {
    var self = this;

    if (self.selectedCount()) {
      self.showMoveSelectedAssets();
    }
    else {
      self.showMoveAsset(assetId);
    }
  },

  moveAssetOrSelectedToTrash: function(assetId) {
    var self = this;

    if (self.selectedCount()) {
      self.moveSelectedToTrash();
    }
    else {
      self.moveToTrash(assetId);
    }
  },

  getUrl: function() {
    var self = this;
    return '/api/v1/query/p/' +  self.currentPage() + '/rpp/' + self.resultsPerPage() + '?' + self.queryString
  },

  search: function(callback) {
    var self = this;

    var opts = {
      'successCallback': function (json) {
        var assets = $.map(json['assets'], function(data) {
          return new Asset(data);
        });

        // remember the focused asset
        var focusedAsset = self.getFocusedAsset();
        var focusedAssetId = focusedAsset ? focusedAsset.id : null;

        self.searchResults(assets);
        self.totalPages(json.totalPages);
        self.totalRecords(json.totalRecords);

        $('.result-box').draggable({
          helper: "clone",
          appendTo: "body",
          opacity: 0.4
        });

        // configure the slider
        $("#slider").slider({
          min: 1,
          max: self.totalPages(),
          value: self.currentPage(),
          slide: function( event, ui ) {
            self.currentPage(ui.value);
            self.search();
          }
        });

        // restore selection
        for (var i = 0; i < self.searchResults().length; i++) {
          var asset = self.searchResults()[i];

          if (asset.id in self.selectedAssetsMap) {
            asset.selected(true);
          }
        }

        // restore focus
        self.focusAssetById(focusedAssetId);

        self.setupResultsHotkeys();

        if (callback && typeof callback === 'function') {
          callback.bind(self).call();
        }
      }
    };

    self.get(self.getUrl(), opts);
  },

  gotoPrevPage: function(callback) {
    if (this.currentPage() < 2) {
      return;
    }
    this.currentPage(this.currentPage() - 1);
    this.search(callback);
  },

  gotoNextPage: function(callback) {
    if (this.currentPage() == this.totalPages()) {
      return;
    }
    this.currentPage(this.currentPage() + 1);
    this.search(callback);
  },

  focusDown: function() {
    var self = this;
    console.log('focusing down');

    var el = self.getFocusedEl();
    if (!el) {
      self.focusFirst();
      return;
    }

    self.moveFocus(el, 'down')
  },

  focusRight: function() {
    var self = this;
    console.log('focusing right');

    var el = self.getFocusedEl();
    if (!el) {
      self.focusFirst();
      return;
    }

    self.moveFocus(el, 'right')
  },

  focusLeft: function() {
    var self = this;
    console.log('focusing left');

    var el = self.getFocusedEl();
    if (!el) {
      self.focusFirst();
      return;
    }

    self.moveFocus(el, 'left')
  },

  focusUp: function() {
    var self = this;
    console.log('focusing up');

    var el = self.getFocusedEl();
    if (!el) {
      self.focusFirst();
      return;
    }

    self.moveFocus(el, 'up')
  },

  getFocusedEl: function() {
    var focusedSel = $('.result-box.focused');
    console.log('Focused element', focusedSel);
    return focusedSel.length ? $(focusedSel[0]) : null;
  },

  getFocusedAsset: function() {
    var self = this;

    var el = self.getFocusedEl();
    if (!el) {
      return null;
    }

    var assetId = el.attr('asset_id');
    return self.getAssetOnPageById(assetId);
  },

  getAssetOnPageById: function(assetId) {
    var self = this;
    return ko.utils.arrayFirst(self.searchResults(), function(asset) {
      return asset.id === assetId;
    })
  },

  focusFirst: function() {
    var self = this;
    console.log('focusing first');
    var el = $("[asset_id='" + self.firstAsset().id + "']");
    console.log('first element', el);
    el.addClass('focused');
    el.focus();
  },

  focusLast: function() {
    var self = this;
    console.log('focusing last');
    var el = $("[asset_id='" + self.lastAsset().id + "']");
    el.addClass('focused');
    el.focus();
  },

  firstAsset: function() {
    var self = this;

    if (!self.searchResults().length) {
      return null;
    }

    return self.searchResults()[0];
  },

  lastAsset: function() {
    var self = this;

    if (!self.searchResults().length) {
      return null;
    }

    return self.searchResults()[self.searchResults().length -1];
  },

  moveFocus: function(curEl, direction) {
    var self = this;

    var currentPos = curEl.position();
    console.log('current');
    console.log(currentPos);
    var height = curEl.height();
    var offset = curEl.offset();

    var newPos = {};
    var elem = null;
    var assetId = null;

    switch (direction) {
      case 'down':
        newPos.left = offset.left + 30;
        newPos.top  = offset.top  + height + 30;
        elem = document.elementFromPoint(newPos.left, newPos.top);
        console.log('element', elem);
        assetId = $(elem).parent().attr('asset_id');
        console.log('asset id', assetId);
        break;

      case 'up':
        newPos.left = offset.left + 30;
        newPos.top  = offset.top - height + 30;
        elem = document.elementFromPoint(newPos.left, newPos.top);
        assetId = $(elem).parent().attr('asset_id');
        break;

      case 'right':
        elem = curEl.next();
        assetId = $(elem).attr('asset_id');
        break;

      case 'left':
        elem = curEl.prev();
        assetId = $(elem).attr('asset_id');
    }

    /*
     Paginate once we hit a boundary
     */
    if (!assetId && direction === 'right') {
      if (self.currentPage() === self.totalPages()) {
        return;
      }

      self.gotoNextPage(self.focusFirst);
    }

    if (!assetId && direction === 'left') {
      if (self.currentPage() === 1) {
        return;
      }

      self.gotoPrevPage(self.focusLast);
    }

    // if down or up is hit on last/first element - also paginate
    if (!assetId && direction === 'down' && curEl.attr('asset_id') === self.lastAsset().id) {
      if (self.currentPage() === self.totalPages()) {
        return;
      }

      self.gotoNextPage(self.focusFirst);
    }

    if (!assetId && direction === 'up' && curEl.attr('asset_id') === self.firstAsset().id) {
      if (self.currentPage() === 1) {
        return;
      }

      self.gotoPrevPage(self.focusLast);
    }

    if (!assetId && (direction === 'up' || direction === 'down')) {
      return;
    }

    // deselect old and select new
    var newFocusedEl = $("[asset_id='" + assetId + "']");
    newFocusedEl.addClass('focused');
    newFocusedEl.focus();
    curEl.removeClass('focused');
  },

  defocusAssetById: function(id) {
    var el = $("[asset_id='" + id + "']");
    el.removeClass('focused');
    el.focus();
  },

  focusAssetById: function(id) {
    var self = this;

    if (!id) {
      return;
    }

    self.clearFocusing();

    var el = $("[asset_id='" + id + "']");
    el.addClass('focused');
    el.focus();
  },

  selectRight: function() {
    var self = this;
    console.log('selecting right');
  },

  selectLeft: function() {
    var self = this;
    console.log('selecting left');
  },

  selectUp: function() {
    var self = this;
    console.log('selecting up');
  },

  selectDown: function() {
    var self = this;
    console.log('selecting down');
  },

  clearFocusing: function() {
    console.log('Clearing focusing');
    $(".focused").removeClass('focused');
  },

  selectFocused: function() {
    var self = this;
    console.log('selecting focused');
    var focusedAsset = self.getFocusedAsset();

    if (!focusedAsset) {
      return;
    }

    if (!(focusedAsset.id in self.selectedAssetsMap)) {
      self.selectedAssetsMap[focusedAsset.id] = focusedAsset;
      self.selectedIds.push(focusedAsset.id);
    }
    focusedAsset.selected(true);
  },

  selectAllOnPage: function() {
    var self = this;
    console.log('select all on page');

    self.searchResults().forEach(function(asset) {
      if (!(asset.id in self.selectedAssetsMap)) {
        self.selectedAssetsMap[asset.id] = asset;
        self.selectedIds.push(asset.id);
      }
      asset.selected(true);
    })
  },

  clearSelection: function() {
    var self = this;
    console.log('clearSelection');

    self.searchResults().forEach(function(asset) {
      asset.selected(false);
    });

    this.selectedAssetsMap = {};
    this.selectedIds([]);
    console.log(this.selectedCount());
  },

  deselectAllOnPage: function() {
    var self = this;
    console.log('deselect all on page');

    self.searchResults().forEach(function(asset) {
      delete self.selectedAssetsMap[asset.id];
      self.selectedIds.remove(asset.id);
      asset.selected(false);
    });

    console.log(this.selectedCount());
  },

  deselectFocused: function() {
    var self = this;
    console.log('deselecting focused');
    var focusedAsset = self.getFocusedAsset();

    if (!focusedAsset) {
      return;
    }

    delete self.selectedAssetsMap[focusedAsset.id];
    self.selectedIds.remove(focusedAsset.id);
    focusedAsset.selected(false);
  },

  showMoveSelectedToTrash: function() {
    var self = this;
    $('#delSelectedAssetsModal').modal();
  },

  moveSelectedToTrash: function() {
    var self = this;

    var assetIds = self.selectedIds();

    var opts = {
      'data': {
        'asset_ids': assetIds
      },
      'successCallback': function() {
        self.loadFolders();
        self.refreshResults();
        self.warning("Assets moved to trash");
        self.clearSelection();
      },
      'finally': function() {
        self.clearSelection();
        $('#delSelectedAssetsModal').modal('hide');
      }
    };

    self.post('/api/v1/trash/recycle', opts);
  },

  _showFolderModal: function(treeEl, actionEl, successFn, showRoot, folderFilterFn) {
    var self = this;

    showRoot = showRoot || false;

    var targetFolderSelected = typeof treeEl.jstree('get_selected')[0] === "string";

    if (targetFolderSelected) {
      actionEl.removeAttr('disabled');
    } else {
      actionEl.attr('disabled','disabled');
    }

    var opts = {
      'successCallback': function (json) {
        var allFolders = json.hierarchy;

        if (folderFilterFn) {
          folderFilterFn(allFolders);
        }

        var hierarchy = [];

        if (showRoot === true) {
          hierarchy.push({
            'id': '0',
            'name': 'Root',
            'children': allFolders
          });
        }
        else {
          hierarchy = allFolders;
        }

        // traverse the hierarchy and "massage" the tree. name -> text
        function _processFolderNode(node) {
          node.text = node.name;
          for (var i = 0; i < node.children.length; ++i) {
            var child = node.children[i];
            _processFolderNode(child);
          }
        }

        for (var i = 0; i < hierarchy.length; ++i) {
          var node = hierarchy[i];
          _processFolderNode(node);
        }

        $.jstree.defaults.core.themes.variant = "large";

        treeEl.jstree({
          'core' : {
            "multiple" : false,
            "animation" : 0,
            "check_callback": true},
          "plugins" : ["search"]
        });

        treeEl.jstree(true).settings.core.data = hierarchy;
        treeEl.jstree(true).refresh();

        successFn(json);
      }
    };

    self.get('/api/v1/folders', opts);
  },

  showMoveSelectedAssets: function() {
    var self = this;

    if (!self.folders().length) {
      self.warning('No folders created yet');
      return;
    }

    var successCallback = function() {
      $('#folderSelModal-moveSelectedAssets').modal();
    };

    var folderFilterFn = function(allFolders) {
      self._removeFolder(self.currentFolderId(), allFolders);
    };

    self._showFolderModal(
        self.moveSelectedAssetsToFolderTreeEl,
        self.moveSelectedAssetsEl,
        successCallback,
        false, /* do not show root folder */
        folderFilterFn);
  },

  showMoveAsset: function(assetId) {
    var self = this;

    if (!self.folders().length) {
      self.warning('No folders created yet');
      return;
    }

    var successCallback = function() {
      self.actionState = assetId;
      $('#folderSelModal-moveAsset').modal();
    };

    var folderFilterFn = function(allFolders) {
      self._removeFolder(self.currentFolderId(), allFolders);
    };

    self._showFolderModal(
        self.moveAssetToFolderTreeEl,
        self.moveAssetEl,
        successCallback,
        false, /* do not show root folder */
        folderFilterFn);
  },

  moveAsset: function() {
    /*
    Move assets based on current state
     */
    var self = this;
    var assetId = self.actionState;
    var moveToFolderId = self.moveAssetToFolderTreeEl.jstree('get_selected')[0];
    self.moveAssetToFolder(assetId, moveToFolderId);
  },



  showMoveFolder: function(folderId) {
    var self = this;

    var successCallback = function() {
      $('#folderSelModal-moveFolder').modal();
    };

    var folderFilterFn = function(allFolders) {
      self._removeFolder(folderId, allFolders);
    };

    self._showFolderModal(
        self.moveToFolderTreeEl,
        self.moveFolderEl,
        successCallback,
        true, /* show root folder */
        folderFilterFn);
  },

  moveSelectedAssets: function() {
    var self = this;
    var moveToFolderId = self.moveSelectedAssetsToFolderTreeEl.jstree('get_selected')[0];
    console.log('move selected assets to ' + moveToFolderId);

    var opts = {
      'data': {
        'asset_ids': self.selectedIds()
      },
      'successCallback': function() {
        self.loadFolders();
        self.clearSelection();
        self.refreshResults();
        if (self.currentFolderId() === '0') {
          self.warning("Assets moved. They may still be visible since you are viewing all assets.");

        } else {
          self.success("Assets moved");
        }
      },
      'finally': function() {
        self.clearSelection();
        $('#folderSelModal-moveSelectedAssets').modal('hide');
      }
    };

    self.post('/api/v1/assets/move/to/' + moveToFolderId, opts);
  },

  moveToTriage: function(assetId) {
    console.log(assetId, 'to triage');

    var self = this;
    var opts = {
      'successCallback': function() {
        self.loadFolders(self.currentFolderId());
        self.success("Asset folder cleared");
      }
    };

    this.post('/api/v1/assets/' + assetId + '/move/to/triage', opts);
  },

  moveToTrash: function(assetId, callback) {
    var self = this;

    console.log(assetId, 'to trash');

    var assetIdx = self.getAssetIndex(assetId);

    var opts = {
      'successCallback': function() {
        delete self.selectedAssetsMap[assetId];
        self.selectedIds.remove(assetId);

        self.searchResults.remove(function(asset) {
          return asset.id === assetId;
        });

        var cb = null;

        if (self.searchResults().length) {
          cb = function() {
            // focus on asset in place of deleted one
            var assetInPlace = assetIdx + 1 > self.searchResults().length ? self.lastAsset() : self.searchResults()[assetIdx];
            self.focusAssetById(assetInPlace.id);
            if (callback) callback();
          };
        }
        else {
          // focus the last asset of reloaded results for seamless focusing
          cb = function() {
            self.focusLast();
            if (callback) callback();
          };
        }

        self.refreshResults(cb);
        self.loadFolders(self.currentFolderId());

        self.warning("Asset moved to trash");
      }
    };

    this.post('/api/v1/trash/recycle/' + assetId, opts);
  },

  showAddFolder: function() {
    var self = this;
    self.resetAllMessages();
    this._showAddFolder(true);
    $('#addFolder').find('input').attr("tabindex",-1).focus();
  },

  hideAddFolder: function() {
    this.resetAddFolderForm();
    this._showAddFolder(false);
  },

  resetAddFolderForm: function() {
    var form = $('#addFolderForm');
    form.find('.has-error').removeClass('has-error').find('.error').text('');
    form.find('input').val('');
  },

  addFolder: function() {
    var self = this;
    var opts = {
      'successCallback': function (json) {
        console.log(json);
        self.hideAddFolder();
        self.loadFolders(self.currentFolderId());
      },
      errorContainerId: 'addFolder',
      data: {
        'name': $('#newFolderName').val(),
        'parent_id': self.currentFolderId()
      }
    };

    this.post('/api/v1/folders', opts);
  },

  goIntoFolder: function(folderId) {
    var self = this;

    self.loadFolders(folderId);
    this.searchResults([]);

    self.queryString = folderId != 0 ? 'folders=' + folderId : '';
    self.currentPage(1);
    self.search();
  },

  loadFolders: function(folderId) {
    var self = this;

    folderId = folderId || self.currentFolderId();

    var folderCallOpts = {
      'successCallback': function (json) {
        var folders = $.map(json['folders'], function(data) {
          return new Folder(data);
        });

        var path = $.map(json['path'], function(data) {
          return new Folder(data);
        });

        self.currentFolderId(folderId);
        self.folders(folders);
        self.currentFolderPath(path);

        var elFolderTargets = $(".folder");
        elFolderTargets.droppable({
          accept: ".result-box",
          hoverClass: "highlight",
          tolerance: "pointer"
        });

        elFolderTargets.on("drop", function( event, ui ) {
          self.resetAllMessages();
          var assetId = $(ui.draggable.context).attr('asset_id');
          var folderId = $(event.target).attr('folder_id');
          self.moveAssetToFolder(assetId, folderId);
        });
      }
    };

    self.get('/api/v1/folders/' + folderId + "/children", folderCallOpts);

    var statsCallOpts = {
      'successCallback': function (json) {
        var stats = json['stats'];
        self.stats.totalAssets(stats.total_assets);
        self.stats.triageAssets(stats.triage_assets);
        self.stats.recycledAssets(stats.recycled_assets);
      }
    };

    self.get('/api/v1/stats', statsCallOpts);
  },

  moveAssetToFolder: function(assetId, folderId) {
    var self = this;

    var opts = {
      'successCallback': function() {
        self.loadFolders(self.currentFolderId());
        self.refreshResults();
        self.success("Asset moved");
      },
      'finally': function() {
        self.actionState = null;
        $('#folderSelModal-moveAsset').modal('hide');
      }
    };

    this.post('/api/v1/assets/' + assetId + '/move/' + folderId, opts);
  },

  refreshResults: function(cb) {
    /*
      Redo the search and if there is nothing here, go one page back (if possible)
     */
    var self = this;

    console.log('Refreshing results');

    var callback = function() {
      if (self.currentPage() > 1 && self.searchResults().length === 0) {
        self.currentPage(self.currentPage() - 1);
        self.search(cb);
      } else {
        if (cb && typeof cb === 'function') {
          cb.bind(self).call();
        }
      }
    };

    self.search(callback);
  },

  showRenameFolder: function(folderId) {
    var self = this;
    self.resetAllMessages();
    var modal = $('#renameFolderModal');
    var folderToRename = $.grep(this.folders(), function(f){ return f.id == folderId; })[0];
    $('#renameFolderInput').val(folderToRename.name);
    $('#renameFolderId').val(folderId);
    modal.modal();
    self.resetFormErrors('#renameFolderForm');
  },

  deleteFolder: function(folderId) {
    var self = this;

    var opts = {
      'successCallback': function() {
        self.loadFolders(self.currentFolderId());
        self.warning("Folder deleted");
      },
      'finally': function() {
        self.actionState = null;
      }
    };

    this.del('/api/v1/folders/' + folderId, opts);
  },

  renameFolder: function() {
    var self = this;
    var folderId = $('#renameFolderId').val();
    var newFolderName = $('#renameFolderInput').val();
    console.log('Renaming folder', folderId, 'with new name', newFolderName);

    var opts = {
      'successCallback': function() {
        self.loadFolders(self.currentFolderId());
        $('#renameFolderModal').modal('hide');
      },
      errorContainerId: 'renameFolderForm',
      'data': {
        'name': newFolderName
      }
    };

    this.put('/api/v1/folders/' + folderId, opts);
  },

  moveFolder: function() {
    var self = this;
    var moveFolderId = self.actionState;
    var moveToFolderId = this.moveToFolderTreeEl.jstree('get_selected')[0];
    console.log('Moving', moveFolderId, 'to', moveToFolderId);

    var opts = {
      'successCallback': function() {
        self.loadFolders(self.currentFolderId());
        self.success("Folder moved");
      },
      'finally': function() {
        self.actionState = null;
        $('#folderSelModal-moveFolder').modal('hide');
      },
      'data': {
        'parent_id': moveToFolderId
      }
    };

    this.put('/api/v1/folders/' + moveFolderId, opts);
  },

  showQuickSelectionView: function() {

  },

  _removeFolder: function(id, hierarchy) {
    for(var i=0; i < hierarchy.length; ++i) {
      var o = hierarchy[i];

      if (o.id == id) {
        hierarchy.splice(i, 1);
        return true;
      }
      else {
        var deleted = this._removeFolder(id, o.children);
        if (deleted) {
          return true;
        }
      }
    }
  },

  getDirectoryNames: function(path) {
    var self = this;
    var opts = {
      'successCallback': function (json) {
        self.directoryNames(json.directory_names);
        self.currentPath(json.current_path);
        self.osDirSeparator(json.os_dir_separator);
      },
      'finally': function() {
        self.disableDoubleClick = false;
      }
    };

    if (path) {
      opts.data = {'path': path}
    }

    // some browsers can fire the doubleclick event when populating the list
    this.disableDoubleClick = true;
    this.get('/import/fs/listing', opts);
  },

  gotoPreviousDirectory: function() {
    var self = this;
    var opts = {
      'successCallback': function (json) {
        self.directoryNames(json.directory_names);
        self.currentPath(json.current_path);
      },
      'finally': function() {
        self.disableDoubleClick = false;
      },
      'data': {
        'path': self.currentPath()}
    };

    // some browsers can fire the doubleclick event when populating the list
    this.disableDoubleClick = true;
    this.get('/import/fs/listing/parent', opts);
  },

  dropIntoDirectory: function(directoryName) {
    if (this.disableDoubleClick == true) {
      return;
    }

    this.getDirectoryNames(this.currentPath() + this.osDirSeparator() + directoryName);
  },

  selectImportDirectory: function() {
    var directoryName = $('#importDirectoryList').val() || '';
    var importDirectoryPath = this.currentPath() == this.osDirSeparator()
        ? this.currentPath()
        : this.currentPath() + this.osDirSeparator() + directoryName;
    window.location = "/client/import?importDirectoryPath=" + importDirectoryPath;
  },

  focusOrSelectAsset: function(view, asset, event) {
    var self = view;

    if (event.shiftKey) {
      if (asset.selected()) {
        // deselect
        delete self.selectedAssetsMap[asset.id];
        self.selectedIds.remove(asset.id);
        asset.selected(false);
      }
      else {
        self.selectedAssetsMap[asset.id] = asset;
        self.selectedIds.push(asset.id);
        asset.selected(true);
      }
    }
    else {
      self.focusAssetById(asset.id);
    }
  },

  showAssetDetail: function(view, asset) {
    var self = view;

    if (!asset) {
      return;
    }

    var opts = {
      'successCallback': function (json) {
        self.detailAsset(new Asset(json.asset));
        $('#assetModal').modal();
        self.focusAssetById(self.detailAsset().id);
        self.setupAssetDetailHotkeys();

        // restore state
        var asset = self.getAssetOnPageById(self.detailAsset().id);
        self.detailAsset().selected(asset.selected());
      }
    };

    this.get('/api/v1/assets/' + asset.id, opts);
  },

  getAssetIndex: function(assetId) {
    var self = this;

    for (var idx = 0; idx < self.searchResults().length; idx++) {
      var a = self.searchResults()[idx];

      if (a.id === assetId) {
        return idx;
      }
    }
  },

  gotoNextAssetDetail: function() {
    var self = this;

    var lastAsset = self.lastAsset();

    if (lastAsset && lastAsset.id === self.detailAsset().id) {
      // no more results
      if (self.currentPage() === self.totalPages()) {
        return;
      }

      self.currentPage(self.currentPage() + 1);

      var showNextAssetDetail = function() {
        var firstAsset = self.firstAsset();
        if (firstAsset) {
          self.showAssetDetail(self, firstAsset);
        }
      };

      self.search(showNextAssetDetail);

      return;
    }

    var idx = self.getAssetIndex(self.detailAsset().id);
    var nextAsset = self.searchResults()[idx + 1];
    self.showAssetDetail(self, nextAsset);
  },

  gotoPrevAssetDetail: function() {
    var self = this;

    var firstAsset = self.firstAsset();

    if (firstAsset && firstAsset.id === self.detailAsset().id) {
      // no more results
      if (self.currentPage() < 2) {
        return;
      }

      self.currentPage(self.currentPage() - 1);

      var showPrevAssetDetail = function() {
        var lastAsset = self.lastAsset();
        if (lastAsset) {
          self.showAssetDetail(self, lastAsset);
        }
      };

      self.search(showPrevAssetDetail);

      return;
    }

    var idx = self.getAssetIndex(self.detailAsset().id);
    var prevAsset = self.searchResults()[idx - 1];
    self.showAssetDetail(self, prevAsset);
  }
});
