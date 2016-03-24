SearchViewModel = BaseViewModel.extend({
  constructor : function() {
    "use strict";

    this.base();
    console.log('Initializing search view model');

    this.searchResults = ko.observableArray();
    this.resultsPerPage = ko.observable(6);
    this.currentPage = ko.observable(1);
    this.totalPages = ko.observable(0);
    this.totalResults = ko.observable(0);
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

    this.get('/api/v1/search/p/' +  self.currentPage() + '/rpp/' + self.resultsPerPage() + '?' + self.queryString, opts);
  }
});
