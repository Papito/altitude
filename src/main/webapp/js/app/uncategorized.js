UncategorizedViewModel = SearchViewModel.extend({
  constructor : function() {
    "use strict";

    this.base();
    var self = this;
    console.log('Initializing uncategorized view model');
  },

  search: function(append, page) {
    var self = this;
    var content = $("#searchResults");

    if (page == 1) {
      content.attr("tabindex", -1).focus();
    }

    var gridAdjustment = Util.getGridAdjustment(
        content, self.resultBoxSize, self.resultBoxBorder
    );
    //console.log(gridAdjustment);
    self.resultBoxMargin(gridAdjustment.boxMargin);
    self.resultBoxPadding(gridAdjustment.boxPadding);
    self.resultBoxWidth(gridAdjustment.boxWidth);
    self.resultBoxSideMargin(gridAdjustment.boxSideMargin);
    self.resultBoxDblSideMargin(self.resultBoxSideMargin() * 2);

    var approxRowsPerPage = parseInt(gridAdjustment.containerHeight / gridAdjustment.boxHeight, 10);
    var rpp = (approxRowsPerPage * gridAdjustment.fitsHorizontally) * 3;

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

    this.get('/api/v1/search/p/' +  page + '/rpp/' + rpp, opts);
  }
});
