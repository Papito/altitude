BrowseViewModel = SearchViewModel.extend({
  constructor : function() {
    "use strict";

    this.base();
    var self = this;
    console.log('Initializing browse view model');

    this.folders = ko.observableArray();
    this.currentFolderPath = ko.observableArray();
    this.currentFolderId = ko.observable(0);
    this._showAddFolder = ko.observable(false);

    this.loadFolders(self.currentFolderId());

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

    // initialize commonly used elements
    this.moveToFolderTreeEl = $('#moveToFolderTree');
    this.moveFolderEl = $('#moveFolder');

    // when a folder is selected, enable the "move" button
    this.moveToFolderTreeEl.bind(
        "select_node.jstree", function(){
          self.moveFolderEl.removeAttr('disabled');
        }
    ); },

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
        'parentId': self.currentFolderId()
      }
    };

    this.post('/api/v1/folders', opts);
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

        var elFolderTargets = $(".folder-target");
        elFolderTargets.droppable({
          accept: ".result-box",
          hoverClass: "highlight",
          tolerance: "pointer"
        });

/*
        elFolderTargets.on("over", function( event, ui ) {
          console.log("OVER", ui);
        });
        elFolderTargets.on("out", function( event, ui ) {
          console.log("OUT", ui);
        });
*/

        elFolderTargets.on("drop", function( event, ui ) {
          console.log("DROPPED", ui);
        });
      }
    };

    this.get('/api/v1/folders/' + folderId, opts);
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
          'name': 'Home',
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