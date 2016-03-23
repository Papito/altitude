SearchViewModel = BaseViewModel.extend({
  constructor : function() {
    "use strict";

    this.base();
    console.log('Initializing search view model');

    this.searchResults = ko.observableArray();
    this.resultsPerPage = 6;

    this.getResultBoxSize();
  },

  search: function(append, page, queryString) {
    var self = this;
    var content = $("#searchResults");

    queryString = queryString || '';
    console.log('q', queryString);

    var opts = {
      'successCallback': function (json) {
        var assets = $.map(json['assets'], function(data) {
          return new Asset(data);
        });

        if (!append) {
          self.searchResults([]);
        }
        for (var idx in assets) {
          var asset = assets[idx];
          self.searchResults.push(asset);
        }
        $('.result-box').draggable({
          helper: "clone",
          appendTo: "body",
          opacity: 0.4
        });

        if (assets.length) {
          $("#searchResults").endlessScroll({
            loader: '<div class="lo:${C.Api.Search.RESULTS_PER_PAGE}">Adding><div>',
            callback: function(){
              self.search(/*append=*/true, /*page=*/page + 1, queryString);
            }
          });
        }
      }
    };

    this.get('/api/v1/search/p/' +  page + '/rpp/' + self.resultsPerPage + '?' + queryString, opts);
  },

  getResultBoxSize: function() {
    var self = this;
    var opts = {
      'successCallback': function (json) {
        self.resultBoxSize = json['result_box_size'];
        self.search(/*append=*/true, /*page=*/1);
      }
    };

    this.get('/api/v1/search/meta/box', opts);
  }
});
