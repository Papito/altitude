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

        this.assets = ko.observableArray([]);
        this.import();
    },

    import: function() {
        var ws = new WebSocket('ws://localhost:9000/ws/import');

        // When the connection is open, send some data to the server
        ws.onopen = function () {
            ws.send('');
            console.log('Socket connected')
        };

        // Log errors
        ws.onerror = function (error) {
            console.log('WebSocket Error ');
            console.log(error);
        };

        ws.onmessage = function (e) {
            if (e.data) {
                var out = '<tr><td>' + e.data + '</td></tr>'
                $('#out').prepend(out);
                ws.send('');
            }
            else {
                ws.close();
            }
        };

    }
});
