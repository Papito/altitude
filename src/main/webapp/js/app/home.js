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
      var opts = {
        'successCallback': this.populateSearchResults
      };
      this.get('/search', opts);
    },

    populateSearchResults: function(data, textStatus, jqXHR) {
    }
});
