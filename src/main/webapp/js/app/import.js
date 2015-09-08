function Asset(data) {
    this.id = data ? data.id : null;
    this.fileName = data ? data.fileName : null;
    this.createdAt = data ? data.createdAt : null;
    this.updatedAt = data ? data.updatedAt : null;
}

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
        this.currentAsset = ko.observable();
        this.importMode = ko.observable();
        this.importProfiles = ko.observableArray();
        this.directoryNames = ko.observableArray();
        this.currentPath = ko.observable();
        this.importDirectory = ko.observable();
        this.disableDoubleClick = false;
        this.responseHandler = null;

        this.percentComplete = ko.computed(function() {
            var percent = 0;
            if (self.assetsImportedCnt() > 0) {
                percent = Math.floor((self.assetsImportedCnt()/ self.totalAssetsCnt()) * 100);
            }
            return percent;
        }, this);

        this.previewUrl = ko.computed(function() {
            if (!self.currentAsset())
                return null;

            return "/assets/" + self.currentAsset().id + "/preview";
        }, this);

        this.currentAssetPath = ko.computed(function() {
            return self.currentAsset() ? self.currentAsset().path : "";
        }, this);

        this.loadImportProfiles();
    },

    cancelImport: function() {
        console.log('Closing websocket');
        this.isImporting(false);
        this.subSocket.close();
        this.currentAsset(null);
        this.totalAssetsCnt(0);
        this.assetsImportedCnt(0);
        this.importMode(null);
        this.importDirectory(null);
        this.currentPath(null);
        $('#imported-assets').html("");
        $("#importMessages").html("");
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

        this.get('/api/ip', opts);
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
            },
            'finally': function() {
                $('#createImportProfile').modal('hide');
            }
        };
        this.post('/api/ip/', opts);
    },

    addWarning: function(asset, message) {
        $("#importMessages").prepend(
            '<div class="message"><button type="button" class="btn btn-warning">' +
            message + '</button>&nbsp;&nbsp;' + asset.path + '</div>');
        this.trimMessages();
    },

    addError: function(asset, message) {
        $("#importMessages").prepend(
            '<div class="message"><button type="button" class="btn btn-danger">' +
            message + '</button>&nbsp;&nbsp;' + asset.absolutePath + '</div>');
        this.trimMessages();
    },

    addCritical: function(asset, message) {
        $("#importMessages").prepend(
            '<div class="message"><button type="button" class="btn btn-danger">' +
            message + '</button>&nbsp;&nbsp;</div>');
        this.trimMessages();
    },

    addSuccess: function(asset) {
        $("#importMessages").prepend(
            '<div class="message">' +
                '<button type="button" class="btn btn-success">Imported</button>' +
                '&nbsp;&nbsp;' + asset.path +
            '</div>');
        this.trimMessages();
    },

    trimMessages: function() {
        var messageCount = $("#importMessages .message").length;

        if (messageCount > 20) {
            var el = $("#importMessages .message").last();
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
        if (json.asset) {
            this.currentAsset(json.asset);
        }

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