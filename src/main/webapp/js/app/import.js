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

        this.socket = null;
        this.isImporting = ko.observable(false);
        this.totalAssetsCnt = ko.observable(0);
        this.assetsImportedCnt = ko.observable(0);
        this.responseHandler = null;

        this.percentComplete = ko.computed(function() {
            var percent = 0;
            if (this.assetsImportedCnt() > 0) {
                percent = Math.floor((this.assetsImportedCnt()/ this.totalAssetsCnt()) * 100);
            }
            return percent;
        }, this);
    },

    cancelImportAssets: function() {
        console.log('Closing websocket');
        this.socket.send("stop");
        this.isImporting(false);
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
            console.log('Socket connected');
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

            console.log('ws > ' + e.data);

            var jsonData = JSON.parse(e.data);
            self.responseHandler(jsonData);

        };
    },

    sendCommand: function(cmd, handler) {
        console.log('ws < ' + cmd);
        this.responseHandler = handler;
        this.socket.send(cmd);
    },

    handleImport: function (json) {
        $('#imported-assets').html(json.asset.path);
        this.assetsImportedCnt(this.assetsImportedCnt() + 1);
    },

    handleTotal: function (json) {
        this.totalAssetsCnt(json.total);
        this.sendCommand('import', this.handleImport);
    }
});
