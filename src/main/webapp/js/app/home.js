function Asset(data) {
  this.id = data ? data.id : null;
  this.path = data? data.path: null;
  this.fileName = data ? data.fileName : null;
  this.createdAt = data ? data.createdAt : null;
  this.updatedAt = data ? data.updatedAt : null;
}

HomeViewModel = BaseViewModel.extend({
    constructor : function() {
        "use strict";

        this.base();
        console.log('Initializing home view model');

        var self = this;
        this.searchResults = ko.observableArray();

        this.searchLatest();
    },

    searchLatest: function() {
      var self = this;
      var opts = {
        'successCallback': function (json) {
          var assets = $.map(json.assets, function(asset) {
            return new Asset(asset);
          });
          self.searchResults(assets);
        }
      };

      this.get('/api/search', opts);
    }
});
