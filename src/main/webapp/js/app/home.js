HomeViewModel = BaseViewModel.extend({
  constructor : function() {
    "use strict";

    this.base();
    console.log('Initializing home view model');

    var self = this;
    this.searchResults = ko.observableArray();

    this.resultBoxBorder = 2; //pixels
    this.resultBoxMargin = ko.observable();
    this.resultBoxSideMargin = ko.observable();
    this.resultBoxPadding = ko.observable();
    this.resultBoxWidth = ko.observable();
    this.searchLatest();
  },

  searchLatest: function() {
    var self = this;
    var opts = {
      'successCallback': function (json) {
        var assets = $.map(json['assets'], function(asset) {
          return new Asset(asset);
        });

        var searchResultsWidth = $("#searchResults").width();
        var viewportW = searchResultsWidth - (searchResultsWidth * 0.025);

        var assetW = json['resultBoxSize'];
        self.resultBoxPadding(parseInt(assetW * 0.05));
        self.resultBoxMargin(parseInt(assetW * 0.05));
        self.resultBoxWidth(assetW +
            (self.resultBoxMargin() + self.resultBoxPadding() + self.resultBoxBorder) * 2);

        var fitsHorizontally = parseInt(viewportW / self.resultBoxWidth(), 10);
        var viewPortRemainder = viewportW - (fitsHorizontally * self.resultBoxWidth());
        var remainderPerSide = parseInt(viewPortRemainder / (fitsHorizontally * 2));

        self.searchResults(assets);
        self.resultBoxSideMargin(self.resultBoxMargin() + remainderPerSide);
        //searchResultsWidth = fitsHorizontally * self.resultBoxWidth() + ((self.resultBoxMargin() + self.resultBoxPadding() + self.resultBoxBorder) *2);
        //console.log("total: " + searchResultsWidth);
      }
    };

    this.get('/api/search', opts);
  }
});
