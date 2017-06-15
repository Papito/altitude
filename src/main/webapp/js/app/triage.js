TriageViewModel = AssetsViewModel.extend({
  constructor : function() {
    "use strict";

    var self = this;
    this.queryString = 'folders=c10000000000000000000000';

    this.base();
    console.log('Initializing triage view model');

    this.folders = ko.observableArray();
  },

  _setupDragDrop: function(startElId) {
    var self = this;

    var elFolderTargets = $('#' + startElId + ' .jstree-anchor');
    elFolderTargets.droppable({
      accept: ".result-box",
      hoverClass: "highlight",
      tolerance: "pointer"
    });

    elFolderTargets.on("drop", function(event, ui) {
      self.resetAllMessages();
      var assetId = $(ui.draggable.context).attr('asset_id');
      var folderId = $(event.target).attr('folder_id');
      self.moveAssetToFolder(assetId, folderId);
    });
  },

  loadFolders: function() {
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
        self._setupDragDrop(treeEl.attr('id'));
      });

      treeEl.off('open_node.jstree');
      treeEl.on('open_node.jstree', function (e, node) {
        self._setupDragDrop(node.node.id);
      });
    };

    folderAddedFn = function(node) {
      // clone the node and add it to the folders observable array
      self.folders.push($.extend({}, node));
      node.icon = "glyphicon glyphicon-folder-close";
      node.data = node.a_attr = {'folder_id': node.id};
      node.li_attr = {'class': 'folder', 'folder_id': node.id};
      node.id = 'triage_node_' + node.id;
    };

    self.folders.removeAll();

    self.loadFolderTree({
      treeEl: treeEl,
      showRoot: false,
      successFn: successFn,
      folderAddedFn: folderAddedFn
    });
  },

  registerFolderContextMenu: function() {
    var self = this;

    $.contextMenu({
      selector: 'li.folder',
      items: {
        add: {
          name: "Add new folder",
          callback: function(key, opt){
            self.resetAllMessages();
            self.actionState = opt.$trigger.context.attributes.getNamedItem('folder_id').nodeValue;
            self.showNewFolderModal();
          }
        },
        rename: {
          name: "Rename folder",
          callback: function(key, opt){
            self.resetAllMessages();
            var folderId = opt.$trigger.context.attributes.getNamedItem('folder_id').nodeValue;
            self.showRenameFolderModal(folderId);
          }
        },
        move: {
          name: "Move folder",
          callback: function(key, opt){
            self.resetAllMessages();
            var folderId = opt.$trigger.context.attributes.getNamedItem('folder_id').nodeValue;
            self.showMoveFolderModal(folderId);
          }
        },
        delete: {
          name: "Delete folder",
          callback: function(key, opt){
            var folderId = opt.$trigger.context.attributes.getNamedItem('folder_id').nodeValue;
            self.deleteFolder(folderId);
          }
        }
      }
    });
  }
});
