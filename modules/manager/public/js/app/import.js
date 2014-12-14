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
            self.socket.send('');
            console.log('Socket connected')
        };

        // Log errors
        this.socket.onerror = function (error) {
            self.isImporting(false);
            console.log('!!! WebSocket Error !!!');
            console.log(error);
        };

        this.socket.onmessage = function (e) {
            if (e.data) {
                var out = '<tr><td>' + e.data + '</td></tr>'
                $('#out').prepend(out);
                self.socket.send('');
            }
            else {
                self.cancelImportAssets();
            }
        };

    }
});
