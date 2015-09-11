jQuery.support.cors = true;

if (!window.console) {
    console = {
        log : function() {}
    };
}

BaseViewModel = Base.extend({
    constructor : function() {
        console.log('Initializing base view model');
        this.base();
    },
    // ----------------------------------------------------------------
    
    post : function(url, opts) {
        return this.restRequest(url, 'POST', opts);
    },
    // ----------------------------------------------------------------

    put : function(url, opts) {
        return this.restRequest(url, 'PUT', opts);
    },
    // ----------------------------------------------------------------

    del : function(url, opts) {
        return this.restRequest(url, 'DELETE', opts);
    },
    // ----------------------------------------------------------------

    get : function(url, opts) {
        return this.restRequest(url, 'GET', opts);
    },
    // ----------------------------------------------------------------

    restRequest : function(url, method, opts) {
        var self = this;

        method = method.toUpperCase();
        
        data = opts.data || {};            

        if (method == 'DELETE' && !opts.successCallback) {
            alert('Cannot call "DELETE" method without successCallback defined');
            return;
        }

        $.ajax({
            type : method,
            url : url,
            crossDomain : true,
            data : data,
            cache : false,
            error : function(jqXHR, textStatus, errorThrown) {
                if (jqXHR.status === 500) {
                    console.log(jqXHR.responseText);
                }
                else if (opts.errorCallback) {
                    opts.errorCallback(jqXHR, textStatus, errorThrown);
                }

                if (opts.finally) {
                    opts.finally();
                }

            },
            success : function(json, textStatus, jqXHR) {
                console.log(json);
                if (opts.successCallback) {
                    opts.successCallback(json, textStatus, jqXHR);
                }

                if (opts.finally) {
                    opts.finally();
                }
            }
        });
    }
});
