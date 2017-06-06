ProtocolHandler = Base.extend({
  constructor : function(socket, viewModel) {
    var self = this;
    this.viewModel = viewModel;

    this.base();

    this.request = {
      url: "/import/fs/ws",
      logLevel: 'debug',
      contentType : "application/json",
      closeAsync: true,
      transport: 'websocket',
      fallbackTransport: 'long-polling'
    };

    this.uid = null;

    this.request.onOpen = function(response) {
      console.log('Atmosphere connected using ' + response.transport);
      console.log("What is our Atmosphere UID?");
      self.sendCommand({'action': 'getUID'})
    };

    this.request.onReconnect = function(rq, rs) {
      self.socket.info("Reconnecting");
    };

    this.request.onClose = function(rs) {
      console.log("Closing connection")
    };

    this.request.onError = function(rs) {
      //FIXME: banner error
      console.log("Socket Error");
      console.log(rs);
    };

    this.socket = socket;
    this.subSocket = null;
  },

  onMessage: function(rs) {
    var self = this;

    console.log(rs);
    var message = rs.responseBody;
    console.log('ws -> ' + message);

    try {
      var json = jQuery.parseJSON(message);
    } catch (e) {
      console.log('This doesn\'t look like a valid JSON, bro: ', message);
    }

    if (json.uid) {
      self.uid = json.uid;
    }

    return json;
  },

  sendCommand: function(message) {
    var json = JSON.stringify(message);
    console.log('ws <- ' + json);
    this.subSocket.push(json);
  }
});
//-----------------------------------------------------------------------------

ImportProtocolHandler = ProtocolHandler.extend({
  constructor : function(socket, viewModel) {
    var self = this;
    this.base(socket, viewModel);

    this.request.onMessage = function(rs) {
      var json = self.onMessage(rs);

      if (json.total) {
        self.viewModel.totalAssetsCnt(json.total);
        self.startImport();
      }

      if (json.imported != null) {
        self.viewModel.assetsProcessedCnt(self.viewModel.assetsProcessedCnt() + 1);

        if (json.warning) {
          self.viewModel.addWarning(json.asset, json.warning);
        }
        else if (json.error) {
          self.viewModel.addError(json.import_asset, json.error);
        }
        else if (json.critical) {
          //FIXME: banner error and exit
          self.viewModel.addCritical(json.critical);
        }
        if (json.imported === true) {
          self.viewModel.addSuccess(json.asset);
        }
      }

      if (json.end) {
        self.viewModel.isImporting(false);
        self.viewModel.isStopping(false);
        self.viewModel.importMode(null);
        self.viewModel.importDirectory(null);
      }
    };

    this.subSocket = this.socket.subscribe(this.request);
  },

  getTotal: function () {
    var self = this;

    var message = {
      'action': "getFileCount",
      'path': self.viewModel.importDirectoryPath()
    };
    self.sendCommand(message);
  },

  startImport: function() {
    var self = this;
    self.viewModel.isImporting(true);

    var message = {
      'action': "startImport"
    };
    self.sendCommand(message);
  },

  stopImport: function() {
    var self = this;

    if (self.viewModel.isStopping()) {
      console.log("void");
      return;
    }

    self.viewModel.isStopping(true);
    var message = {
      'action': "stopImport"
    };
    self.sendCommand(message);
  }
});
//-----------------------------------------------------------------------------

ImportViewModel = BaseViewModel.extend({
  constructor : function() {
    "use strict";
    var self = this;

    this.IMPORT_MODE = {
      'DIRECTORY': 'directory',
      'FILES':  'files'
    };

    this.base();
    console.log('Initializing import view model');

    this.importDirectoryPath = ko.observable();
    this.isImporting = ko.observable(false);
    this.isStopping = ko.observable(false);
    this.totalAssetsCnt = ko.observable(0);
    this.assetsProcessedCnt = ko.observable(0);
    this.statsImported = ko.observable(0);
    this.statsWarnings = ko.observable(0);
    this.statsErrors = ko.observable(0);
    this.importMode = ko.observable();

    var args = Util.parseQuery(location.search);
    if (args.importDirectoryPath) {
      this.importMode(this.IMPORT_MODE.DIRECTORY);
      this.importDirectoryPath(args.importDirectoryPath);
    }

    this.percentComplete = ko.computed(function() {
      var percent = 0;
      if (self.assetsProcessedCnt() > 0) {
        percent = Math.floor((self.assetsProcessedCnt()/ self.totalAssetsCnt()) * 100);
      }
      return percent;
    }, this);

    console.log('Initialized Atmosphere');
    var socket = $.atmosphere;
    this.protocol = new ImportProtocolHandler(socket, self);
    console.log('Import protocol handler attached');
  },

  cancelImport: function() {
    this.protocol.stopImport();
  },

  reset: function() {
    console.log('Resetting');
    this.isImporting(false);
    this.isStopping(false);
    this.totalAssetsCnt(0);
    this.assetsProcessedCnt(0);
    this.statsImported(0);
    this.statsWarnings(0);
    this.statsErrors(0);
    $("#importedAssets").html("");
    $("#errors").html("");
    $("#warnings").html("");
  },

  addWarning: function(asset, message) {
    $("#warnings").prepend(
        '<div><strong>' +  message + '</strong>: ' + asset.path + '</div>');
    this.statsWarnings(this.statsWarnings() + 1);
  },

  addError: function(importAsset, message) {
    var path = importAsset ? importAsset['absolutePath'] : 'Error. See log for details.';

    $("#errors").prepend(
        '<div><strong>' +  message + '</strong>: ' + path + '</div>');
    this.statsErrors(this.statsErrors() + 1);
  },

  //FIXME: should display global error banner message and bail
  addCritical: function(asset, message) {
    $("#importedAssets").prepend(
        '<div class="result-box"><button type="button" class="btn btn-danger">' +
        message + '</button>&nbsp;&nbsp;</div>');
  },

  addSuccess: function(asset) {
    $("#importedAssets").html(
        '<div class="result-box">' +
        '<img src="/assets/' + asset.id + '/preview">' +
        '</div>');
    this.statsImported(this.statsImported() + 1);
  },

  importAssets: function() {
    this.reset();
    this.protocol.getTotal();
  }
});