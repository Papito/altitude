TriageViewModel = AssetsViewModel.extend({
  constructor : function() {
    "use strict";

    var self = this;

    this.base();
    console.log('Initializing triage view model');

    this.folders = ko.observableArray();
  },

  _setFolderDropTargets: function(startElId) {
    var self = this;

    var elFolderTargets = $('#' + startElId + ' .jstree-anchor').not('.root');
    elFolderTargets.droppable({
      accept: ".result-box",
      hoverClass: "highlight",
      tolerance: "pointer"
    });

    elFolderTargets.on("drop", function(event, ui) {
      var assetId = $(ui.draggable.context).attr('asset_id');
      var folderId = $(event.target).attr('folder_id');
      self.moveAssetToFolder(assetId, folderId);
    });
  },

  getUrl: function() {
    var self = this;
    return '/api/v1/query/triage/p/' +  self.currentPage() + '/rpp/' + self.resultsPerPage() + '?' + self.queryString
  },

  moveAssetToFolder: function(assetId, folderId, reloadFolders) {
    var self = this;

    var opts = {
      'successCallback': function() {
        self.refreshResults();
        self.loadStats();
        self.success("Asset moved");
      },
      'finally': function() {
        self.actionState = null;
        $('#moveAssetModal').modal('hide');
      }
    };

    this.post('/api/v1/assets/' + assetId + '/move/' + folderId, opts);
  },


  loadFolders: function(nodeIdToExpand) {
    var self = this;

    var treeEl = $('#triageFolderTree');

    var successFn = function() {
      treeEl.off('after_open.jstree');
      treeEl.on('after_open.jstree', function (e, data) {
        data.instance.set_icon(data.node, "glyphicon glyphicon-folder-open");
      });

      treeEl.off('close_node.jstree');
      treeEl.on('close_node.jstree', function (e, data) {
        data.instance.set_icon(data.node, "glyphicon glyphicon-folder-close");
      });

      treeEl.off('refresh.jstree');
      treeEl.on('refresh.jstree', function () {
        self._setFolderDropTargets(treeEl.attr('id'));

        if (nodeIdToExpand) {
          treeEl.jstree("open_node", $('#triage_node_' + nodeIdToExpand));
        }
      });

      treeEl.off('open_node.jstree');
      treeEl.on('open_node.jstree', function (e, node) {
        self._setFolderDropTargets(node.node.id);
      });
    };

    var folderAddedFn = function(node) {
      // clone the node and add it to the folders observable array
      self.folders.push($.extend({}, node));
      node.icon = "glyphicon glyphicon-folder-close";
      node.data = node.a_attr = {'folder_id': node.id};

      if (node.isRoot) {
        node.state = {'opened' : true};
        node.li_attr = {'class': 'folder root', 'folder_id': node.id};
        node.a_attr = {'class': 'root'};
      }
      else {
        node.li_attr = {'class': 'folder', 'folder_id': node.id};
      }

      node.id = 'triage_node_' + node.id;
    };

    self.folders.removeAll();

    self.loadFolderTree({
      treeEl: treeEl,
      showRoot: false,
      successFn: successFn,
      folderAddedFn: folderAddedFn
    });

    self.loadStats();
  },

  addFolderViaModal: function() {
    var self = this;

    var parentId = self.actionState;
    var modalEl = $('#newFolderModal');

    var opts = {
      'successCallback': function() {
        modalEl.modal('hide');
        self.loadFolders(parentId);
      },
      errorContainerId: 'newFolderModal-newFolderForm',
      data: {
        'name': $('#newFolderModal-newFolderName').val(),
        'parent_id': parentId
      }
    };

    this.post('/api/v1/folders', opts);
  },

  registerFolderContextMenu: function() {
    var self = this;

    $.contextMenu({
      selector: 'li.folder',
      items: {
        add: {
          name: "New subfolder",
          callback: function(key, opt){
            self.resetAllMessages();
            self.actionState = opt.$trigger.context.attributes.getNamedItem('folder_id').nodeValue;
            self.showNewFolderModal();
          }
        },
        rename: {
          name: "Rename",
          callback: function(key, opt){
            self.resetAllMessages();
            var folderId = opt.$trigger.context.attributes.getNamedItem('folder_id').nodeValue;
            self.showRenameFolderModal(folderId);
          }
        },
        move: {
          name: "Move",
          callback: function(key, opt){
            self.resetAllMessages();
            var folderId = opt.$trigger.context.attributes.getNamedItem('folder_id').nodeValue;
            self.showMoveFolderModal(folderId);
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
  }
});
