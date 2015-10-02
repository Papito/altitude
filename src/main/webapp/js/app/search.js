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

    this.getResultBoxSize();
  },

  search: function(resultBoxSize) {
    var self = this;
    var queryString = window.location.search;

    var content = $("#content");
    var searchResultsWidth = content.width();
    var searchResultsHeight = content.height();

    var viewportW = searchResultsWidth - (searchResultsWidth * 0.025);

    self.resultBoxPadding(parseInt(resultBoxSize * 0.05));
    self.resultBoxMargin(parseInt(resultBoxSize * 0.05));

    self.resultBoxWidth(resultBoxSize +
        (self.resultBoxMargin() + self.resultBoxPadding() + self.resultBoxBorder) * 2);

    var fitsHorizontally = parseInt(viewportW / self.resultBoxWidth(), 10);
    var viewPortRemainder = viewportW - (fitsHorizontally * self.resultBoxWidth());
    var remainderPerSide = parseInt(viewPortRemainder / (fitsHorizontally * 2));

    var resultBoxHeight = (resultBoxSize + (self.resultBoxPadding() + self.resultBoxBorder) * 2);

    var approxRowsPerPage = parseInt(searchResultsHeight / resultBoxHeight, 10);
    var rpp = (approxRowsPerPage * fitsHorizontally) * 2

    var opts = {
      'successCallback': function (json) {
        var assets = $.map(json['assets'], function(asset) {
          return new Asset(asset);
        });

        self.searchResults(assets);
        self.resultBoxSideMargin(self.resultBoxMargin() + remainderPerSide);
      }
    };

    this.get('/api/v1/search' + queryString + "&rpp=" + rpp, opts);
  },

  getResultBoxSize: function() {
    var self = this;
    var opts = {
      'successCallback': function (json) {
        self.search(json['resultBoxSize']);
      }
    };

    this.get('/api/v1/search/meta/box', opts);
  }
});
