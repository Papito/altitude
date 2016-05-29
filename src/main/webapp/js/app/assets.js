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
    this.uncategorizedFolder = ko.observable(new Folder({name: ''}));
    this.trashFolder = ko.observable(new Folder({name: ''}));
    this.currentFolderPath = ko.observableArray();
    this.currentFolderId = ko.observable("0");
    this._showAddFolder = ko.observable(false);

    this.selectedAssetsMap = {};
    this.selectedIds = ko.observableArray();

    this.selectedCount = ko.computed(function() {
      return this.selectedIds().length;
    }, this);

    this.queryString = this.queryString || '';
    console.log('Q = ', this.queryString);

    this.prevPageVisible = ko.computed(function() {
      return this.currentPage() > 1;
    }, this);

    this.nextPageVisible = ko.computed(function() {
      return this.currentPage() < this.totalPages();
    }, this);


    this.moveAssetsToFolderTreeEl = $('#moveAssetsToFolderTree');
    this.moveAssetsEl = $('#moveAssets');

    // when a folder is selected, enable the "move" button
    this.moveAssetsToFolderTreeEl.bind(
        "select_node.jstree", function(){
          self.moveAssetsEl.removeAttr('disabled');
        }
    );

    // set up shortcuts
    Mousetrap.bind(['.', 'pagedown'], function() {
      self.gotoNextPage();
    }, 'keyup');

    Mousetrap.bind([',', 'pageup'], function() {
      self.gotoPrevPage();
    }, 'keyup');

    Mousetrap.bind('right', function() {
      self.focusRight();
    }, 'keyup');

    Mousetrap.bind('left', function() {
      self.focusLeft();
    }, 'keyup');

    Mousetrap.bind('up', function() {
      self.focusUp();
    }, 'keyup');

    Mousetrap.bind('down', function() {
      self.focusDown();
    }, 'keyup');

    Mousetrap.bind('shift+right', function() {
      self.selectRight();
    });

    Mousetrap.bind('shift+left', function() {
      self.selectLeft();
    });

    Mousetrap.bind('shift+up', function() {
      self.selectUp();
    });

    Mousetrap.bind('shift+down', function() {
      self.selectDown();
    });

    Mousetrap.bind('esc', function() {
      self.clearFocusing();
    });

    Mousetrap.bind('s', function() {
      self.selectFocused();
    });

    Mousetrap.bind('d', function() {
      self.deselectFocused();
    });

    Mousetrap.bind('shift+s', function() {
      self.selectAll();
    });

    Mousetrap.bind('shift+d', function() {
      self.deselectAll();
    });

    $('#renameFolderForm').on('submit', function(e) {
      e.preventDefault();
      self.renameFolder();
    });

    $('#addFolderForm').on('submit', function(e) {
      e.preventDefault();
      self.addFolder();
    });

    $('#renameFolderModal').on('shown.bs.modal', function () {
      $('#renameFolderInput').focus().select();
    });

    // register the context menu
    $.contextMenu({
      selector: 'span.context-menu',
      items: {
        rename: {
          name: "Rename",
          callback: function(key, opt){
            var folderId = opt.$trigger.context.attributes.getNamedItem('folder_id').nodeValue;
            self.showRenameFolder(folderId);
          }
        },
        move: {
          name: "Move",
          callback: function(key, opt){
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

    // initialize commonly used elements
    this.moveToFolderTreeEl = $('#moveToFolderTree');
    this.moveFolderEl = $('#moveFolder');
    this.uncategorizedEl = $('#uncategorized');
    this.trashEl = $('#trash');

    // when a folder is selected, enable the "move" button
    this.moveToFolderTreeEl.bind(
        "select_node.jstree", function(){
          self.moveFolderEl.removeAttr('disabled');
        }
    );

    /*
     system folders
     */

    // uncategorized
    this.uncategorizedEl.droppable({
      accept: ".result-box",
      hoverClass: "highlight",
      tolerance: "pointer"
    });

    this.uncategorizedEl.on("drop", function( event, ui ) {
      var assetId = $(ui.draggable.context).attr('asset_id');
      self.moveToUncategorized(assetId);
    });

    // trash
    this.trashEl.droppable({
      accept: ".result-box",
      hoverClass: "highlight",
      tolerance: "pointer"
    });

    this.trashEl.on("drop", function( event, ui ) {
      var assetId = $(ui.draggable.context).attr('asset_id');
      self.moveToTrash(assetId);
    });

    // get the data
    this.search();
    this.loadFolders(self.currentFolderId());
  },

  search: function(callback) {
    var self = this;

    var opts = {
      'successCallback': function (json) {
        var assets = $.map(json['assets'], function(data) {
          return new Asset(data);
        });

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

        if (callback && typeof callback === 'function') {
          callback.bind(self).call();
        }

        // restore selection
        for (var i = 0; i < self.searchResults().length; i++) {
          var asset = self.searchResults()[i];

          if (asset.id in self.selectedAssetsMap) {
            asset.selected(true);
          }
        }


      }
    };

    this.get('/api/v1/search/p/' +  self.currentPage() + '/rpp/' + self.resultsPerPage() + '?' + self.queryString, opts);
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
    return focusedSel.length ? $(focusedSel[0]) : null;
  },

  getFocusedAsset: function() {
    var self = this;

    var el = self.getFocusedEl();
    if (!el) {
      return null;
    }

    var assetId = el.attr('asset_id');
    return ko.utils.arrayFirst(self.searchResults(), function(asset) {
      return asset.id === assetId;
    });
  },

  focusFirst: function() {
    var self = this;
    console.log('focusing first');
    var el = $("[asset_id='" + self.firstAsset().id + "']");
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
        newPos.left = offset.left + 10;
        newPos.top  = offset.top  + height + 20;
        elem = document.elementFromPoint(newPos.left, newPos.top);
        console.log('element');
        console.log(elem);
        assetId = $(elem).parent().attr('asset_id');
        console.log('asset id');
        console.log(assetId);
        break;

      case 'up':
        newPos.left = offset.left + 10;
        newPos.top  = offset.top - height + 20;
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

    console.log('new');
    console.log(newPos);

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

  selectAll: function() {
    var self = this;
    console.log('select all');

    self.searchResults().forEach(function(asset) {
      if (!(asset.id in self.selectedAssetsMap)) {
        self.selectedAssetsMap[asset.id] = asset;
        self.selectedIds.push(asset.id);
      }
      asset.selected(true);
    })
  },

  deselectAll: function() {
    var self = this;
    console.log('deselect all');

    self.searchResults().forEach(function(asset) {
      delete self.selectedAssetsMap[asset.id];
      self.selectedIds.remove(function (id) {
        return id === asset.id;
      });
      asset.selected(false);
    })
  },

  deselectFocused: function() {
    var self = this;
    console.log('deselecting focused');
    var focusedAsset = self.getFocusedAsset();

    if (!focusedAsset) {
      return;
    }

    delete self.selectedAssetsMap[focusedAsset.id];
    self.selectedIds.remove(function (id) {
      return id === focusedAsset.id;
    });
    focusedAsset.selected(false);
  },

  deleteSelectedAssets: function() {
    console.log('deleting selected');
  },

  clearSelection: function() {
    this.deselectAll();
    this.selectedAssetsMap = {};
    this.selectedIds();
  },

  showMoveSelectedAssets: function() {
    var self = this;
    console.log('Moving assets');

    var targetSelected = typeof $('#moveAssetsToFolderTree').jstree('get_selected')[0] === "string";

    if (targetSelected) {
      self.moveAssetsEl.removeAttr('disabled');
    } else {
      self.moveAssetsEl.attr('disabled','disabled');
    }

    var opts = {
      'successCallback': function (json) {
        var allFolders = json.hierarchy;

        if (allFolders.length === 0) {
          self.blinkWarning("No possible folders to move to");
          return;
        }

        var hierarchy = [{
          'id': '0',
          'name': 'Root',
          'children': allFolders
        }];

        console.log(hierarchy);

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

        self.moveAssetsToFolderTreeEl.jstree({
          'core' : {
            "multiple" : false,
            "animation" : 0,
            "check_callback": true},
          "plugins" : ["search"]
        });

        self.moveAssetsToFolderTreeEl.jstree(true).settings.core.data = hierarchy;
        self.moveAssetsToFolderTreeEl.jstree(true).refresh();
        $('#selectAssetMoveModal').modal();
      }
    };

    self.get('/api/v1/folders', opts);
  },

  moveSelectedAssets: function() {
    var self = this;
    var moveToFolderId = self.moveAssetsToFolderTreeEl.jstree('get_selected')[0];
    console.log('move selected assets to ' + moveToFolderId);

    var opts = {
      'data': {
        'asset_ids': self.selectedIds()
      },
      'successCallback': function() {
        self.loadFolders();
        self.blinkSuccess("Assets moved");
      },
      'finally': function() {
        self.clearSelection();
        $('#selectAssetMoveModal').modal('hide');
      }
    };

    self.post('/api/v1/assets/move/to/' + moveToFolderId, opts);
  },

  moveToUncategorized: function(assetId) {
    console.log(assetId, 'to uncategorized');

    var self = this;
    var opts = {
      'successCallback': function() {
        self.loadFolders(self.currentFolderId());
        self.blinkSuccess("Asset folder cleared");
      }
    };

    this.post('/api/v1/assets/' + assetId + '/move/to/uncategorized', opts);
  },

  moveToTrash: function(assetId) {
    console.log(assetId, 'to trash');

    var self = this;
    var opts = {
      'successCallback': function() {
        self.loadFolders(self.currentFolderId());
        self.blinkWarning("Asset moved to trash");
      }
    };

    this.post('/api/v1/assets/' + assetId + '/move/to/trash', opts);
  },

  showAddFolder: function() {
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

    var opts = {
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

        var uncategorizedFolder = new Folder(json['system']['1']);
        self.uncategorizedFolder(uncategorizedFolder);
        var trashFolder = new Folder(json['system']['2']);
        self.trashFolder(trashFolder);

        var elFolderTargets = $(".folder-target");
        elFolderTargets.droppable({
          accept: ".result-box",
          hoverClass: "highlight",
          tolerance: "pointer"
        });

        elFolderTargets.on("drop", function( event, ui ) {
          var assetId = $(ui.draggable.context).attr('asset_id');
          var folderId = $(event.target).find('span').attr('folder_id');
          self.moveToFolder(assetId, folderId);
        });
      }
    };

    this.get('/api/v1/folders/' + folderId + "/children", opts);
  },

  moveToFolder: function(assetId, folderId) {
    var self = this;
    var opts = {
      'successCallback': function() {
        self.loadFolders(self.currentFolderId());
        self.blinkSuccess("Asset moved");
      }
    };

    this.post('/api/v1/assets/' + assetId + '/move/' + folderId, opts);
  },

  showRenameFolder: function(folderId) {
    var self = this;
    console.log('Renaming', folderId);
    var modal = $('#renameFolderModal');
    var folderToRename = $.grep(this.folders(), function(f){ return f.id == folderId; })[0];
    $('#renameFolderInput').val(folderToRename.name);
    $('#renameFolderId').val(folderId);
    modal.modal();
    self.resetFormErrors('#renameFolderForm');
  },

  showMoveFolder: function(folderId) {
    var self = this;
    $('#moveFolderId').val(folderId);
    console.log('Moving', folderId);

    var targetSelected = typeof $('#moveToFolderTree').jstree('get_selected')[0] === "string";

    if (targetSelected) {
      self.moveFolderEl.removeAttr('disabled');
    } else {
      self.moveFolderEl.attr('disabled','disabled');
    }

    var opts = {
      'successCallback': function (json) {
        var allFolders = json.hierarchy;

        self._removeFolder(folderId, allFolders);

        if (allFolders.length === 0) {
          self.blinkWarning("No possible folders to move to");
          return;
        }

        var hierarchy = [{
          'id': '0',
          'name': 'Root',
          'children': allFolders
        }];

        console.log(hierarchy);

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

        self.moveToFolderTreeEl.jstree({
          'core' : {
            "multiple" : false,
            "animation" : 0,
            "check_callback": true},
          "plugins" : ["search"]
        });

        self.moveToFolderTreeEl.jstree(true).settings.core.data = hierarchy;
        self.moveToFolderTreeEl.jstree(true).refresh();
        $('#selectFolderMoveModal').modal();
      }
    };

    this.get('/api/v1/folders', opts);
  },

  deleteFolder: function(folderId) {
    var self = this;
    console.log('Deleting', folderId);

    var opts = {
      'successCallback': function() {
        self.loadFolders(self.currentFolderId());
        self.blinkSuccess("Folder deleted");
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
    var moveFolderId = $('#moveFolderId').val();
    var moveToFolderId = this.moveToFolderTreeEl.jstree('get_selected')[0];
    console.log('Moving', moveFolderId, 'to', moveToFolderId);

    var opts = {
      'successCallback': function() {
        self.loadFolders(self.currentFolderId());
        self.blinkSuccess("Folder moved");
      },
      'finally': function() {
        $('#selectFolderMoveModal').modal('hide');
      },
      'data': {
        'parent_id': moveToFolderId
      }
    };

    this.put('/api/v1/folders/' + moveFolderId, opts);
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
  }
});
