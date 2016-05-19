TrashViewModel = AssetsViewModel.extend({
  constructor : function() {
    "use strict";

    this.queryString = 'folders=2';

    this.base();
    console.log('Initializing trash view model');

  }

});
