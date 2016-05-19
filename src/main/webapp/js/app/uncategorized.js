UncategorizedViewModel = AssetsViewModel.extend({
  constructor : function() {
    "use strict";

    this.queryString = 'folders=1';

    this.base();
    console.log('Initializing uncategorized view model');
  }
});
