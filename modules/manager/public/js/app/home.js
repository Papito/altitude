function Asset(data) {
    var self = this;
    this.id = data ? data.id : null;
    this.fileName = data ? data.fileName : null;
    this.createdAt = data ? data.createdAt : null;
    this.updatedAt = data ? data.updatedAt : null;
}

HomeViewModel = BaseViewModel.extend({
    constructor : function() {
        var self = this;
        this.base();
        console.log('Initializing home view model');

        this.assets = ko.observableArray([]);

        this.loadAssets();
    },

    loadAssets: function() {
        var self = this;

        opts = {};
        opts.url = "/api/home"
        opts.successCallback = function(data, textStatus, jqXHR) {
            var assets = $.map(data.assets, function(asset) { return new Asset(asset) });
            self.assets(assets);
        };

        self.get(opts);
    },
});
