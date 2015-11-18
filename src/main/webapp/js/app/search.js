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

    $('#addFolderForm').on('submit', function(e) {
      e.preventDefault();
      self.addFolder();
    });

    // register the context menu
    $.contextMenu({
      selector: 'span.context-menu',
      items: {
        rename: {
          name: "Rename",
          callback: function(key, opt){
            var folderId = opt.$trigger.context.attributes.getNamedItem('folder-id').nodeValue;
            self.renameFolder(folderId);
          }
        },
        move: {
          name: "Move",
          callback: function(key, opt){
            var folderId = opt.$trigger.context.attributes.getNamedItem('folder-id').nodeValue;
            self.moveFolder(folderId);
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
  },

  renameFolder: function(folderId) {
    console.log('Renaming', folderId);
  },

  moveFolder: function(folderId) {
    console.log('Moving', folderId);
  },

  deleteFolder: function(folderId) {
    console.log('Deleting', folderId);
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
  }
});
