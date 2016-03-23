TrashViewModel = SearchViewModel.extend({
  constructor : function() {
    "use strict";

    this.base();
    var self = this;
    console.log('Initializing trash view model');
  },

  search: function(append, page) {
    var self = this;
    var content = $("#searchResults");

    var queryString = window.location.search;
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
          self.searchResults.push(assets[idx]);
        }

        if (assets.length) {
          $("#searchResults").endlessScroll({
            loader: '<div class="loading"><div>',
            callback: function(){
              self.search(/*append=*/true, /*page=*/page + 1);
            }
          });
        }
      }
    };

    this.get('/api/v1/search/p/' +  page + '/rpp/' + this.resultsPerPage + '?folders=2', opts);
  }
});
