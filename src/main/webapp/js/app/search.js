SearchViewModel = BaseViewModel.extend({
  constructor : function() {
    "use strict";

    this.base();
    console.log('Initializing search view model');

    this.searchResults = ko.observableArray();

    this.resultBoxBorder = 2; //pixels
    this.resultBoxMargin = ko.observable();
    this.resultBoxSideMargin = ko.observable();
    this.resultBoxPadding = ko.observable();
    this.resultBoxWidth = ko.observable();
    this.resultBoxSize = null;

    this.getResultBoxSize();
  },

  search: function(append, page) {
    var self = this;
    var content = $("#content");

    if (page == 1) {
      content.attr("tabindex",-1).focus();
    }

    var queryString = window.location.search;

    var searchResultsWidth = content.width();
    var searchResultsHeight = content.height();

    var viewportW = searchResultsWidth - (searchResultsWidth * 0.025);

    self.resultBoxPadding(parseInt(self.resultBoxSize * 0.05));
    self.resultBoxMargin(parseInt(self.resultBoxSize * 0.05));

    self.resultBoxWidth(self.resultBoxSize +
        (self.resultBoxMargin() + self.resultBoxPadding() + self.resultBoxBorder) * 2);

    var fitsHorizontally = parseInt(viewportW / self.resultBoxWidth(), 10);
    var viewPortRemainder = viewportW - (fitsHorizontally * self.resultBoxWidth());
    var remainderPerSide = parseInt(viewPortRemainder / (fitsHorizontally * 2));

    var resultBoxHeight = (self.resultBoxSize +
      (self.resultBoxPadding() + self.resultBoxBorder) * 2);

    var approxRowsPerPage = parseInt(searchResultsHeight / resultBoxHeight, 10);
    var rpp = (approxRowsPerPage * fitsHorizontally) * 3;

    var opts = {
      'successCallback': function (json) {
        var assets = $.map(json['assets'], function(asset) {
          return new Asset(asset);
        });

        if (!append) {
          self.searchResults([]);
        }
        for (var idx in assets) {
          self.searchResults.push(assets[idx]);
        }
        self.resultBoxSideMargin(self.resultBoxMargin() + remainderPerSide);

        if (assets.length) {
          $("#content").endlessScroll({
            loader: '<div class="loading"><div>',
            callback: function(p){
              self.search(/*append=*/true, /*page=*/page + 1);
            }
          });
        }
      }
    };

    this.get('/api/v1/search' + queryString + "&rpp=" + rpp + "&p=" + page, opts);
  },

  getResultBoxSize: function() {
    var self = this;
    var opts = {
      'successCallback': function (json) {
        self.resultBoxSize = json['resultBoxSize'];
        self.search(/*append=*/true, /*page=*/1);
      }
    };

    this.get('/api/v1/search/meta/box', opts);
  }
});
