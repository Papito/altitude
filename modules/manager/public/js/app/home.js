function Asset(data) {
    this.id = data ? data.id : null;
    this.fileName = data ? data.fileName : null;
    this.createdAt = data ? data.createdAt : null;
    this.updatedAt = data ? data.updatedAt : null;
}

HomeViewModel = BaseViewModel.extend({
    constructor : function() {
        this.base();
        console.log('Initializing home view model');

        this.assets = ko.observableArray([]);

        this.loadAssets();
    },

    loadAssets: function() {
        var self = this;

        var opts = {};
        opts.url = "/api"
        opts.successCallback = function(data, textStatus, jqXHR) {
            //var assets = $.map(data.assets, function(asset) { return new Asset(asset) });
            //self.assets(assets);
        };

        self.get(opts);
    },
});
