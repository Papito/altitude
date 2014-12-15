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
        this.assets = ko.observableArray([]);
        this.responseHandler = null;
    },

    cancelImportAssets: function() {
        console.log('Closing websocket');
        this.isImporting(false);
        this.socket.close();
        this.socket = null;
        $('#out').html("");
    },

    importAssets: function() {
        var self = this;
        // FIXME: http://stackoverflow.com/questions/10406930/how-to-construct-a-websocket-uri-relative-to-the-page-uri
        this.socket = new WebSocket('ws://localhost:9000/ws/import');

        // When the connection is open, send some data to the server
        this.socket.onopen = function () {
            self.isImporting(true);

            self.responseHandler = self.handleTotal;
            self.sendCommand('start');

            console.log('Socket connected')
        };

        // Log errors
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

            var jsonData = JSON.parse(e.data);
            console.log('ws > ' + e.data);
            self.responseHandler(jsonData);

        };

    },

    sendCommand: function(cmd) {
        console.log('ws < ' + cmd);
        this.socket.send(cmd);
    },

    handleAsset: function (json) {
        var out = '<tr><td>' + json.asset.path + '</td></tr>';
        $('#out').prepend(out);
        self.responseHandler = self.handleAsset;
        this.sendCommand('next');
    },

    handleTotal: function (json) {
        self.responseHandler = self.handleAsset;
        this.sendCommand('next');
    }
});
