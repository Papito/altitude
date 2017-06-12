TriageViewModel = AssetsViewModel.extend({
  constructor : function() {
    "use strict";

    var self = this;
    this.queryString = 'folders=c10000000000000000000000';

    this.base();
    console.log('Initializing triage view model');

    this.folders = ko.observableArray();
    this.treeEl = $('#triageFolderTree');

    /* Toggle between folder open and folder closed */
    this.treeEl.on('open_node.jstree', function (e, data) {
      data.instance.set_icon(data.node, "glyphicon glyphicon-folder-open"); });
    this.treeEl.on('close_node.jstree', function (e, data) {
      data.instance.set_icon(data.node, "glyphicon glyphicon-folder-close"); });

    this.treeEl.on('refresh.jstree', function () {
      self._setupDragDrop(self.treeEl.attr('id'));
    });

    this.treeEl.on('open_node.jstree', function (e, node) {
      self._setupDragDrop(node.node.id);
    });
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

    var opts = {
      'successCallback': function (json) {
        var hierarchy = json.hierarchy;
        self.folders.removeAll();

        // traverse the hierarchy and "massage" the tree. name -> text
        function _processFolderNode(node) {
          // clone the node and add it to the folders observable array
          self.folders.push($.extend({}, node));

          node.text = node.name;
          node.icon = "glyphicon glyphicon-folder-close";
          node.data = node.a_attr = {'folder_id': node.id};
          node.li_attr = {'class': 'folder', 'folder_id': node.id};
          node.id = 'triage_node_' + node.id;

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

        self.treeEl.jstree({
          'core' : {
            "multiple" : false,
            "animation" : 0,
            "check_callback": true
          },
          "plugins" : ["search"]
        });

        self.treeEl.jstree(true).settings.core.data = hierarchy;
        self.treeEl.jstree(true).refresh();

        console.log(self.folders());
      }
    };

    self.get('/api/v1/folders', opts);

  },

  _registerFolderContextMenu: function() {
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
