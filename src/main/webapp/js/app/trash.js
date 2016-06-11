TrashViewModel = AssetsViewModel.extend({
  constructor : function() {
    "use strict";

    this.base();
    console.log('Initializing trash view model');
  },

  getUrl: function() {
    var self = this;
    return '/api/v1/trash/p/' +  self.currentPage() + '/rpp/' + self.resultsPerPage()
  }

});
