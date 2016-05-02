SearchViewModel = BaseViewModel.extend({
  constructor : function() {
    "use strict";

    var self = this;

    this.base();
    console.log('Initializing search view model');

    this.searchResults = ko.observableArray();
    this.resultsPerPage = ko.observable(15);
    this.currentPage = ko.observable(1);
    this.totalPages = ko.observable(0);
    this.totalRecords = ko.observable(0);

    this.selectedAssetIndexes = ko.observableArray();
    this.focusedAssetIndex = ko.observable();

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
    Mousetrap.bind(['.', 'pagedown'], function() {
      self.gotoNextPage();
    }, 'keyup');

    Mousetrap.bind([',', 'pageup'], function() {
      self.gotoPrevPage();
    }, 'keyup');

    Mousetrap.bind('right', function() {
      self.focusRight();
    }, 'keyup');

    Mousetrap.bind('left', function() {
      self.focusLeft();
    }, 'keyup');

    Mousetrap.bind('up', function() {
      self.focusUp();
    }, 'keyup');

    Mousetrap.bind('down', function() {
      self.focusDown();
    }, 'keyup');

    Mousetrap.bind('shift+right', function() {
      self.selectRight();
    });

    Mousetrap.bind('shift+left', function() {
      self.selectLeft();
    });

    Mousetrap.bind('shift+up', function() {
      self.selectUp();
    });

    Mousetrap.bind('shift+down', function() {
      self.selectDown();
    });

    Mousetrap.bind('esc', function() {
      self.clearFocusing();
    });

    Mousetrap.bind('s', function() {
      self.selectFocused();
    });

  },

  search: function() {
    var self = this;

    var opts = {
      'successCallback': function (json) {
        var assets = $.map(json['assets'], function(data) {
          return new Asset(data);
        });

        self.clearFocusing();

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

  focusDown: function() {
    var self = this;
    console.log('focusing down');

    var el = self.getFocusedEl();
    if (!el) {
      self.focusFirst();
      return;
    }

    self.moveFocus(el, 'down')
  },

  focusRight: function() {
    var self = this;
    console.log('focusing right');

    var el = self.getFocusedEl();
    if (!el) {
      self.focusFirst();
      return;
    }

    self.moveFocus(el, 'right')
  },

  focusLeft: function() {
    var self = this;
    console.log('focusing left');

    var el = self.getFocusedEl();
    if (!el) {
      self.focusFirst();
      return;
    }

    self.moveFocus(el, 'left')
  },

  focusUp: function() {
    var self = this;
    console.log('focusing up');

    var el = self.getFocusedEl();
    if (!el) {
      self.focusFirst();
      return;
    }

    self.moveFocus(el, 'up')
  },

  getFocusedEl: function() {
    var focusedSel = $('.result-box.focused');
    return focusedSel.length ? $(focusedSel[0]) : null;
  },

  focusFirst: function() {
    console.log('focusing first');
    var self = this;
    var el = $("[asset_id='" + self.searchResults()[0].id + "']");
    el.addClass('focused');
    el.focus();
  },

  moveFocus: function(curEl, direction) {
    var currentPos = curEl.position();
    var height = curEl.height();
    var offset = $('#searchResults').offset();

    var newPos = {};
    var elem = null;
    var assetId = null;

    switch (direction) {
      case 'down':
        newPos.left = offset.left + currentPos.left;
        newPos.top  = offset.top + currentPos.top + height + 10;
        elem = document.elementFromPoint(newPos.left, newPos.top);
        assetId = $(elem).parent().attr('asset_id');
        break;

      case 'up':
        newPos.left = offset.left + currentPos.left;
        newPos.top  = offset.top + currentPos.top - height + 10;
        elem = document.elementFromPoint(newPos.left, newPos.top);
        assetId = $(elem).parent().attr('asset_id');
        break;

      case 'right':
        elem = curEl.next();
        assetId = $(elem).attr('asset_id');
        break;

      case 'left':
        elem = curEl.prev();
        assetId = $(elem).attr('asset_id');
    }

    console.log('offset', offset);
    console.log('current pos', currentPos);
    console.log('new pos', newPos);
    console.log('new elem', elem);

    // out of bounds - do nothing
    if (!assetId) {
      return;
    }

    // deslect old and select new
    var newFocusedEl = $("[asset_id='" + assetId + "']");
    newFocusedEl.addClass('focused');
    newFocusedEl.focus();
    curEl.removeClass('focused');
  },

  defocusAssetById: function(id) {
    var el = $("[asset_id='" + id + "']");
    el.removeClass('focused');
    el.focus();
  },

  focusAssetById: function(id) {
    var el = $("[asset_id='" + id + "']");
    el.addClass('focused');
    el.focus();
  },

  selectRight: function() {
    var self = this;
    console.log('selecting right');

    if (self.focusedAssetIndex() == null) {
      self.focusAsset();
    }
  },

  selectLeft: function() {
    var self = this;
    console.log('selecting left');
  },

  selectUp: function() {
    var self = this;
    console.log('selecting up');
  },

  selectDown: function() {
    var self = this;
    console.log('selecting down');
  },

  focusAsset: function() {
    console.log("focusing");
    var self = this;

    console.log("focused asset index", self.focusedAssetIndex());

    // highlight
    $("[asset_id='" + self.searchResults()[self.focusedAssetIndex()].id + "']").addClass('focused');
  },

  clearFocusing: function() {
    var self = this;

    if (!self.focusedAssetIndex()  || !self.searchResults().length) {
      return;
    }

    console.log('Clearing focusing');

    $("[asset_id='" + self.searchResults()[self.focusedAssetIndex()].id + "']").removeClass('focused');
    self.focusedAssetIndex(null);
  },

  selectFocused: function() {
    console.log('selecting focused');
  },
/*
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
*/

  getFocusedAsset: function() {
    var self = this;

    if (self.focusedAssetIndex() == null) {
      return null;
    }

    return ko.utils.arrayFirst(self.searchResults(), function(asset) {
      return asset.id ===  self.focusedAsset().id;
    });
  }



});
