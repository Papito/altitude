function Asset(data) {
    this.id = data ? data.id : null;
    this.fileName = data ? data.fileName : null;
    this.createdAt = data ? data.createdAt : null;
    this.updatedAt = data ? data.updatedAt : null;
}

ImportViewModel = BaseViewModel.extend({
    constructor : function() {
        this.base();
        console.log('Initializing import view model');

        var self = this;
        this.socket = null;
        this.isImporting = ko.observable(false);
        this.totalAssetsCnt = ko.observable(0);
        this.assetsImportedCnt = ko.observable(0);
        this.currentAsset = ko.observable();
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

        this.currentPath = ko.computed(function() {
            return self.currentAsset() ? self.currentAsset().path : "";
        }, this);
    },

    cancelImportAssets: function() {
        console.log('Closing websocket');
        this.isImporting(false);
        this.currentAsset(null);
        this.socket.close();
        this.socket = null;
        this.totalAssetsCnt(0);
        this.assetsImportedCnt(0);
        $('#imported-assets').html("");
    },

    importAssets: function() {
        var self = this;
        // FIXME: http://stackoverflow.com/questions/10406930/how-to-construct-a-websocket-uri-relative-to-the-page-uri
        this.socket = new WebSocket('ws://localhost:8080/import/ws');

        this.socket.onopen = function () {
            self.isImporting(true);
            console.log('Socket connected')
            self.sendCommand('total', self.handleTotal);
        };

        this.socket.onerror = function (error) {
            self.isImporting(false);
            console.log('!!! WebSocket Error !!!');
            console.log(error);
        };

        this.socket.onmessage = function (e) {
            if (!e.data) {
                self.cancelImportAssets();
                return;
            }
            //console.log('ws > ' + e.data);

            var jsonData = JSON.parse(e.data);
            self.responseHandler(jsonData);

        };
    },

    sendCommand: function(cmd, handler) {
        console.log('ws < ' + cmd);
        this.responseHandler = handler;
        this.socket.send(cmd);
    },

    handleAsset: function (json) {
        if (json.asset) {
            this.currentAsset(json.asset);
            this.assetsImportedCnt(this.assetsImportedCnt() + 1);
        }
        this.sendCommand('next', this.handleAsset);
    },

    handleTotal: function (json) {
        this.totalAssetsCnt(json.total);
        this.sendCommand('next', this.handleAsset);
    }
});