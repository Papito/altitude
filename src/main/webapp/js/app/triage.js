TriageViewModel = AssetsViewModel.extend({
  constructor : function() {
    "use strict";

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
  },

  loadFolders: function() {
    var self = this;


    var opts = {
      'successCallback': function (json) {
        var hierarchy = json.hierarchy;
        self.folders(hierarchy);

        // traverse the hierarchy and "massage" the tree. name -> text
        function _processFolderNode(node) {
          node.text = node.name;
          node.icon = "glyphicon glyphicon-folder-close";
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

      }
    };

    self.get('/api/v1/folders', opts);

  }
});
