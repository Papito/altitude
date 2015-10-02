function ImportProfile(data) {
  this.id = data ? data.id : null;
  this.name = data ? data.name : null;
  this.keyword = data ? data.keyword : null;
  this.createdAt = data ? data.createdAt : null;
  this.updatedAt = data ? data.updatedAt : null;
}

ImportViewModel = BaseViewModel.extend({
  constructor : function() {
    "use strict";

    this.IMPORT_MODE = {
      'DIRECTORY': 'directory',
      'FILES':  'files'
    };

    this.base();
    console.log('Initializing import view model');

    var self = this;
    this.isImporting = ko.observable(false);
    this.totalAssetsCnt = ko.observable(0);
    this.assetsImportedCnt = ko.observable(0);
    this.importMode = ko.observable();
    //this.importProfiles = ko.observableArray();
    this.directoryNames = ko.observableArray();
    this.currentPath = ko.observable();
    this.importDirectory = ko.observable();
    this.statsImported = ko.observable(0);
    this.statsWarnings = ko.observable(0);
    this.statsErrors = ko.observable(0);
    this.disableDoubleClick = false;
    this.responseHandler = null;

    this.percentComplete = ko.computed(function() {
      var percent = 0;
      if (self.assetsImportedCnt() > 0) {
        percent = Math.floor((self.assetsImportedCnt()/ self.totalAssetsCnt()) * 100);
      }
      return percent;
    }, this);

    //this.loadImportProfiles();
  },

  cancelImport: function() {
    console.log('Closing websocket');
    this.isImporting(false);
    this.subSocket.close();
    this.totalAssetsCnt(0);
    this.assetsImportedCnt(0);
    this.importMode(null);
    this.importDirectory(null);
    this.statsImported(0);
    this.statsWarnings(0);
    this.statsErrors(0);
    $('#imported-assets').html("");
    $("#importedAssets").html("");
    $("#errors").html("");
    $("#warnings").html("");
  },

  getDirectoryNames: function(path) {
    var self = this;
    var opts = {
      'successCallback': function (json) {
        self.directoryNames(json.directoryNames);
        self.currentPath(json.currentPath);
      },
      'finally': function() {
        self.disableDoubleClick = false;
      }
    };

    if (path) {
      opts.data = {'path': path}
    }

    // some browsers can fire the doubleclick event when populating the list
    this.disableDoubleClick = true;
    this.get('/import/source/local/navigate', opts);
  },

  dropIntoDirectory: function(directoryName) {
    if (this.disableDoubleClick == true) {
      return;
    }
    this.getDirectoryNames(this.currentPath() + "/" + directoryName);
  },

  selectImportDirectory: function() {
    this.importMode(this.IMPORT_MODE.DIRECTORY);
    var directoryName = $('#directoryList').val();
    this.importDirectory(this.currentPath() + "/" + directoryName);
    $('#selectImportDirectory').modal('hide');
  },

  /*
   loadImportProfiles: function() {
   var self = this;
   var opts = {
   'successCallback': function (json) {
   var importProfiles = $.map(json.importProfiles, function(ip) {
   return new ImportProfile(ip);
   });
   self.importProfiles(importProfiles);
   }
   };

   this.get('/api/v1/ip', opts);
   },

   createImportProfile: function() {
   var self = this;

   var data = {
   'name': $("#createImportProfile input:text[name=name]").val(),
   'keywords': $("#createImportProfile input:text[name=keywords]").val()
   };

   var opts = {
   'data': data,
   'successCallback': function (json) {
   // relaod
   self.loadImportProfiles();
   $('#createImportProfile').modal('hide');
   },
   'errorContainer': 'createImportProfileError'
   };
   this.post('/api/ip/', opts);
   },
   */

  addWarning: function(asset, message) {
    $("#warnings").prepend(
        '<div><strong>' +  message + '</strong>: ' + asset.path + '</div>');
    this.statsWarnings(this.statsWarnings() + 1);
    this.trimMessages();
  },

  addError: function(asset, message) {
    $("#errors").prepend(
        '<div><strong>' +  message + '</strong>: ' + asset['absolutePath'] + '</div>');
    this.statsErrors(this.statsErrors() + 1);
    this.trimMessages();
  },

  //FIXME: should display global error banner message and bail
  addCritical: function(asset, message) {
    $("#importedAssets").prepend(
        '<div class="asset"><button type="button" class="btn btn-danger">' +
        message + '</button>&nbsp;&nbsp;</div>');
    this.trimMessages();
  },

  addSuccess: function(asset) {
    $("#importedAssets").prepend(
        '<div class="asset">' +
        '<button type="button" class="btn btn-success">' +
        '<img src="/assets/' + asset.id + '/preview">' +
        '</button></div>');
    this.statsImported(this.statsImported() + 1);
    this.trimMessages();
  },

  trimMessages: function() {
    var messageCount = $("#importedAssets .asset").length;

    if (messageCount > 20) {
      var el = $("#importedAssets .asset").last();
      $(el).remove();
    }
  },

  importAssets: function() {
    var self = this;

    this.socket = $.atmosphere;

    this.request = {
      url: "/import/ws",
      contentType: "text/plain",
      logLevel: 'debug',
      transport: 'websocket',
      fallbackTransport: 'long-polling'
    };
    console.log('Initialized Atmosphere');

    this.request.onOpen = function(response) {
      console.log('Atmosphere connected using ' + response.transport);
      self.isImporting(true);
      self.sendCommand('total ' + self.importDirectory(), self.handleTotal);
    };

    this.request.onReconnect = function(rq, rs) {
      self.socket.info("Reconnecting");
    };

    this.request.onMessage = function(rs) {
      console.log(rs);
      var message = rs.responseBody;
      console.log('ws > ' + message);

      if (!message || message == "END") {
        self.cancelImport();
        return;
      }

      try {
        var json = jQuery.parseJSON(message);
        self.responseHandler(json);
      } catch (e) {
        console.log('This doesn\'t look like a valid JSON object: ', message);
      }
    };

    this.request.onClose = function(rs) {
      console.log("Closing connection")
    };

    this.request.onError = function(rs) {
      //FIXME: banner error
      self.cancelImport();
      console.log("Socket Error");
      console.log(rs);
    };

    this.subSocket = self.socket.subscribe(self.request);
  },

  sendCommand: function(cmd, handler) {
    console.log('ws < ' + cmd);
    this.responseHandler = handler;
    this.subSocket.push(cmd);
  },

  handleAsset: function (json) {
    this.assetsImportedCnt(this.assetsImportedCnt() + 1);
    if (json.warning) {
      this.addWarning(json.asset, json.warning);
    }
    else if (json.error) {
      this.addError(json.importAsset, json.error);
    }
    else if (json.critical) {
      //FIXME: banner error
      this.addCritical(json.asset, json.critical);
    }
    else {
      this.addSuccess(json.asset);
    }

    this.sendCommand('next', this.handleAsset);
  },

  handleTotal: function (json) {
    this.totalAssetsCnt(json.total);
    this.sendCommand('next', this.handleAsset);
  }
});