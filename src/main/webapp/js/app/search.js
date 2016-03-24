SearchViewModel = BaseViewModel.extend({
  constructor : function() {
    "use strict";

    this.base();
    console.log('Initializing search view model');

    this.searchResults = ko.observableArray();
    this.resultsPerPage = 6;
    this.currentPage = 1;
    this.totalPages = 0;
    this.queryString = this.queryString || '';
    console.log('Q = ', this.queryString);

    this.search();
  },

  search: function() {
    var self = this;

    var opts = {
      'successCallback': function (json) {
        var assets = $.map(json['assets'], function(data) {
          return new Asset(data);
        });

        self.searchResults(assets);
      }
    };

    this.get('/api/v1/search/p/' +  self.currentPage + '/rpp/' + self.resultsPerPage + '?' + self.queryString, opts);
  }
});
