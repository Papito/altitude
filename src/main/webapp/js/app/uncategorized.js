UncategorizedViewModel = BrowseViewModel.extend({
  constructor : function() {
    "use strict";

    this.base();
    console.log('Initializing uncategorized view model');
  },

  search: function(append, page) {
    this.base(append, page, "folders=1");
  }
});
