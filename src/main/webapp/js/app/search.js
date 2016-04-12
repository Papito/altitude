SearchViewModel = BaseViewModel.extend({
  constructor : function() {
    "use strict";

    var self = this;

    this.base();
    console.log('Initializing search view model');

    this.searchResults = ko.observableArray();
    this.resultsPerPage = ko.observable(21);
    this.currentPage = ko.observable(1);
    this.totalPages = ko.observable(0);
    this.totalRecords = ko.observable(0);

    this.selectedAssets = ko.observableArray();
    this.focusedAsset = ko.observable();

    this.queryString = this.queryString || '';
    console.log('Q = ', this.queryString);

    this.prevPageVisible = ko.computed(function() {
      return this.currentPage() > 1;
    }, this);

    this.nextPageVisible = ko.computed(function() {
      return this.currentPage() < this.totalPages();
    }, this);

    this.search();

    // set up shortcuts
    Mousetrap.bind(['right', 'down'], function() {
      self.gotoNextPage();
    });

    Mousetrap.bind(['left', 'up'], function() {
      self.gotoPrevPage();
    });

    Mousetrap.bind('s', function() {
      self.selectAsset();
    });

  },

  search: function() {
    var self = this;

    var opts = {
      'successCallback': function (json) {
        var assets = $.map(json['assets'], function(data) {
          return new Asset(data);
        });

        self.searchResults(assets);
        self.totalPages(json.totalPages);
        self.totalRecords(json.totalRecords);

        $('.result-box').draggable({
          helper: "clone",
          appendTo: "body",
          opacity: 0.4
        });

        // configure the slider
        $("#slider").slider({
          min: 1,
          max: self.totalPages(),
          value: self.currentPage(),
          slide: function( event, ui ) {
            self.currentPage(ui.value);
            self.search();
          }
        });


      }
    };

    this.get('/api/v1/search/p/' +  self.currentPage() + '/rpp/' + self.resultsPerPage() + '?' + self.queryString, opts);
  },

  gotoPrevPage: function() {
    if (this.currentPage() < 2) {
      return;
    }
    this.currentPage(this.currentPage() - 1);
    this.search();
  },

  gotoNextPage: function() {
    if (this.currentPage() == this.totalPages()) {
      return;
    }
    this.currentPage(this.currentPage() + 1);
    this.search();
  },

  focusAsset: function() {
    console.log("focusing");
    var self = this;

    if (!self.searchResults()) {
      console.log("no assets");
      return;
    }

    if (!self.focusedAsset()) {
      console.log("no focused asset");
      self.focusedAsset(self.searchResults()[0]);
    }

    console.log("focused asset", self.focusedAsset());

    // highlight
    $("[asset_id='" + self.focusedAsset().id + "']").addClass('focused');
  },

  selectAsset: function() {
    console.log("selecting asset");
    var self = this;

    if (!self.focusedAsset()) {
      self.focusAsset();
    }


    var focusedAsset = self.getFocusedAsset();
    self.selectedAssets().push(focusedAsset);

    // highlight
    self.selectedAssets().forEach(function(asset) {
      $("[asset_id='" + asset.id + "']").addClass('selected');
    });
  },

  getFocusedAsset: function() {
    var self = this;

    if (!self.focusedAsset()) {
      return null;
    }

    return ko.utils.arrayFirst(self.searchResults(), function(asset) {
      return asset.id ===  self.focusedAsset().id;
    });
  }



});
