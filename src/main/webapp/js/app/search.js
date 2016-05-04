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

    this.selectedAssets = {};

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

    Mousetrap.bind('d', function() {
      self.deselectFocused();
    });

    Mousetrap.bind('shift+s', function() {
      self.selectAll();
    });

    Mousetrap.bind('shift+d', function() {
      self.deselectAll();
    });
  },

  search: function(callback) {
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

        if (callback && typeof callback === 'function') {
          callback.bind(self).call();
        }

        // restore selection
        for (var i = 0; i < self.searchResults().length; i++) {
          var asset = self.searchResults()[i];

          if (asset.id in self.selectedAssets) {
            asset.selected(true);
        }
      }


      }
    };

    this.get('/api/v1/search/p/' +  self.currentPage() + '/rpp/' + self.resultsPerPage() + '?' + self.queryString, opts);
  },

  gotoPrevPage: function(callback) {
    if (this.currentPage() < 2) {
      return;
    }
    this.currentPage(this.currentPage() - 1);
    this.search(callback);
  },

  gotoNextPage: function(callback) {
    if (this.currentPage() == this.totalPages()) {
      return;
    }
    this.currentPage(this.currentPage() + 1);
    this.search(callback);
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

  getFocusedAsset: function() {
    var self = this;

    var el = self.getFocusedEl();
    if (!el) {
      return null;
    }

    var assetId = el.attr('asset_id');
    return ko.utils.arrayFirst(self.searchResults(), function(asset) {
      return asset.id === assetId;
    });
  },

  focusFirst: function() {
    var self = this;
    console.log('focusing first');
    var el = $("[asset_id='" + self.firstAsset().id + "']");
    el.addClass('focused');
    el.focus();
  },

  focusLast: function() {
    var self = this;
    console.log('focusing last');
    var el = $("[asset_id='" + self.lastAsset().id + "']");
    el.addClass('focused');
    el.focus();
  },

  firstAsset: function() {
    var self = this;

    if (!self.searchResults().length) {
      return null;
    }

    return self.searchResults()[0];
  },

  lastAsset: function() {
    var self = this;

    if (!self.searchResults().length) {
      return null;
    }

    return self.searchResults()[self.searchResults().length -1];
  },

  moveFocus: function(curEl, direction) {
    var self = this;

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

    /*
      Paginate once we hit a boundary
     */
    if (!assetId && direction === 'right') {
      if (self.currentPage() === self.totalPages()) {
        return;
      }

      self.gotoNextPage(self.focusFirst);
    }

    if (!assetId && direction === 'left') {
      if (self.currentPage() === 1) {
        return;
      }

      self.gotoPrevPage(self.focusLast);
    }

    // if down or up is hit on last/first element - also paginate
    if (!assetId && direction === 'down' && curEl.attr('asset_id') === self.lastAsset().id) {
      if (self.currentPage() === self.totalPages()) {
        return;
      }

      self.gotoNextPage(self.focusFirst);
    }

    if (!assetId && direction === 'up' && curEl.attr('asset_id') === self.firstAsset().id) {
      if (self.currentPage() === 1) {
        return;
      }

      self.gotoPrevPage(self.focusLast);
    }

    if (!assetId && (direction === 'up' || direction === 'down')) {
      return;
    }

    // deselect old and select new
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

  clearFocusing: function() {
    console.log('Clearing focusing');
    $(".focused").removeClass('focused');
  },

  selectFocused: function() {
    var self = this;
    console.log('selecting focused');
    var focusedAsset = self.getFocusedAsset();

    if (!focusedAsset) {
      return;
    }

    self.selectedAssets[focusedAsset.id] = focusedAsset;
    focusedAsset.selected(true);
  },

  selectAll: function() {
    var self = this;
    console.log('select all');

    self.searchResults().forEach(function(asset) {
      self.selectedAssets[asset.id] = asset;
      asset.selected(true);
    })
  },

  deselectAll: function() {
    var self = this;
    console.log('deselect all');

    self.searchResults().forEach(function(asset) {
      delete self.selectedAssets[asset.id];
      asset.selected(false);
    })
  },

  deselectFocused: function() {
    var self = this;
    console.log('deselecting focused');
    var focusedAsset = self.getFocusedAsset();

    if (!focusedAsset) {
      return;
    }

    delete self.selectedAssets[focusedAsset.id];
    focusedAsset.selected(false);
  }

});
