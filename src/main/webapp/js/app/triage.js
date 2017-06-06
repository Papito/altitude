TriageViewModel = AssetsViewModel.extend({
  constructor : function() {
    "use strict";

    this.queryString = 'folders=c10000000000000000000000';

    this.base();
    console.log('Initializing triage view model');
  },

  setupFolderNav: function() {
    var self = this;
    self.folders = ko.observableArray();
  }
});
