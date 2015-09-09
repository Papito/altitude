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
