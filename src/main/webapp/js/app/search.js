SearchViewModel = BaseViewModel.extend({
  constructor : function() {
    "use strict";

    this.base();
    var self = this;
    console.log('Initializing search view model');

    this.searchResults = ko.observableArray();
    this.folders = ko.observableArray();
    this.currentFolderPath = ko.observableArray();
    this.currentFolderId = ko.observable(0);

    this._showAddFolder = ko.observable(false);

    this.resultBoxBorder = 2; //pixels
    this.resultBoxMargin = ko.observable();
    this.resultBoxSideMargin = ko.observable();
    this.resultBoxDblSideMargin = ko.observable();
    this.resultBoxPadding = ko.observable();
    this.resultBoxWidth = ko.observable();
    this.resultBoxSize = null;

    this.getResultBoxSize();
    this.loadFolders(self.currentFolderId());

    this.rootFolder = new Folder({name: 'Root', id: '0'});

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
            var folderId = opt.$trigger.context.attributes.getNamedItem('folder-id').nodeValue;
            self.showRenameFolder(folderId);
          }
        },
        move: {
          name: "Move",
          callback: function(key, opt){
            var folderId = opt.$trigger.context.attributes.getNamedItem('folder-id').nodeValue;
            self.showMoveFolder(folderId);
          }
        },
        delete: {
          name: "Delete",
          callback: function(key, opt){
            var folderId = opt.$trigger.context.attributes.getNamedItem('folder-id').nodeValue;
            self.deleteFolder(folderId);
          }
        }
      }
    });

    // predefined elements
    this.moveToFolderTreeEl = $('#moveToFolderTree');
  },

  search: function(append, page) {
    var self = this;
    var content = $("#searchResults");

    if (page == 1) {
      content.attr("tabindex", -1).focus();
    }

    var gridAdjustment = Util.getGridAdjustment(
        content, self.resultBoxSize, self.resultBoxBorder
    );
    //console.log(gridAdjustment);
    self.resultBoxMargin(gridAdjustment.boxMargin);
    self.resultBoxPadding(gridAdjustment.boxPadding);
    self.resultBoxWidth(gridAdjustment.boxWidth);
    self.resultBoxSideMargin(gridAdjustment.boxSideMargin);
    self.resultBoxDblSideMargin(self.resultBoxSideMargin() * 2);

    var approxRowsPerPage = parseInt(gridAdjustment.containerHeight / gridAdjustment.boxHeight, 10);
    var rpp = (approxRowsPerPage * gridAdjustment.fitsHorizontally) * 3;

    var queryString = window.location.search;

    var opts = {
      'successCallback': function (json) {
        var assets = $.map(json['assets'], function(data) {
          return new Asset(data);
        });

        if (!append) {
          self.searchResults([]);
        }
        for (var idx in assets) {
          self.searchResults.push(assets[idx]);
        }

        if (assets.length) {
          $("#searchResults").endlessScroll({
            loader: '<div class="loading"><div>',
            callback: function(p){
              self.search(/*append=*/true, /*page=*/page + 1);
            }
          });
        }
      }
    };

    this.get('/api/v1/search' + queryString + "&rpp=" + rpp + "&p=" + page, opts);
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
      data: {
        'name': $('#newFolderName').val(),
        'parentId': self.currentFolderId()
      }
    };

    this.post('/api/v1/folders', opts);
  },

  getResultBoxSize: function() {
    var self = this;
    var opts = {
      'successCallback': function (json) {
        self.resultBoxSize = json['resultBoxSize'];
        self.search(/*append=*/true, /*page=*/1);
      }
    };

    this.get('/api/v1/search/meta/box', opts);
  },

  loadFolders: function(folderId) {
    var self = this;
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
        self.currentFolderPath.unshift(self.rootFolder);
      }
    };

    this.get('/api/v1/folders/' + folderId, opts);
  },

  showRenameFolder: function(folderId) {
    console.log('Renaming', folderId);
    var modal = $('#renameFolderModal');
    var folderToRename = $.grep(this.folders(), function(f){ return f.id == folderId; })[0];
    $('#renameFolderInput').val(folderToRename.name);
    $('#renameFolderId').val(folderId);
    modal.modal();
  },

  showMoveFolder: function(folderId) {
    var self = this;
    $('#moveFolderId').val(folderId);
    console.log('Moving', folderId);

    var opts = {
      'successCallback': function (json) {
        var hierarchy = json.hierarchy;

        self._removeFolder(folderId, hierarchy);

        if (hierarchy.length === 0) {
          self.blinkWarning("No possible folders to move to");
          return;
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

        self.moveToFolderTreeEl.jstree({
          'core' : {
            "multiple" : false,
            "animation" : 0,
            "check_callback": true},
          "plugins" : ["search"]
        });

        self.moveToFolderTreeEl.jstree(true).settings.core.data = hierarchy;
        self.moveToFolderTreeEl.jstree(true).refresh();
        $('#selectFolderToMoveToModal').modal();
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
      },
      'finally': function() {
        $('#renameFolderModal').modal('hide');
      },
      'data': {
        'name': newFolderName
      }
    };

    this.put('/api/v1/folders/' + folderId, opts);
  },

  moveFolder: function() {
    var self = this;
    var moveFolderId = $('#moveFolderId').val();
    var moveToFofolderId = this.moveToFolderTreeEl.jstree('get_selected')[0];
    console.log('Moving', moveFolderId, 'to', moveToFofolderId);

    var opts = {
      'successCallback': function() {
        self.loadFolders(self.currentFolderId());
        self.blinkSuccess("Folder moved");
      },
      'finally': function() {
        $('#selectFolderToMoveToModal').modal('hide');
      },
      'data': {
        'parentId': moveToFofolderId
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
