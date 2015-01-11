jQuery.support.cors = true;

if (!window.console) {
    console = {
        log : function() {}
    };
}

BaseViewModel = Base.extend({
    constructor : function() {
        var self = this;
        console.log('Initializing base view model');
        this.base();

        // we use this to visually select where we are
        this.currentTab = ko.observable();

        this.loading = $('#loadingDiv').hide();

        this.errorMessage = ko.observable();
        this.warningMessage = ko.observable();
        this.successMessage = ko.observable();
        this.infoMessage = ko.observable();

        // read success message and show on every page load
        this.getSuccessMessage();
    },
    // ----------------------------------------------------------------
    
    post : function(opts) {
        return this.restRequest('POST', opts);
    },
    // ----------------------------------------------------------------

    put : function(opts) {
        return this.restRequest('PUT', opts);
    },
    // ----------------------------------------------------------------

    del : function(opts) {
        return this.restRequest('DELETE', opts);
    },
    // ----------------------------------------------------------------

    get : function(opts) {
        return this.restRequest('GET', opts);
    },
    // ----------------------------------------------------------------
    

    clearMessages: function() {
        var self = this;

        self.errorMessage('');
        self.warningMessage('');
        self.infoMessage('');
        self.successMessage('');
    },

    restRequest : function(method, opts) {
        var self = this;

        method = method.toUpperCase();
        
        data = opts.data || {};            

        if (method == 'DELETE' && !opts.successCallback) {
            alert('Cannot call "DELETE" method without successCallback defined');
            return;
        }

        self.clearMessages();

        $.ajax({
            type : method,
            url : opts.url,
            crossDomain : true,
            data : data,
            cache : false,
            error : function(jqXHR, textStatus, errorThrown) {
                $('.has-error').removeClass('has-error');
                /*
                    Nasty error
                if (jqXHR.status === 500) {
                    $("#errorMessageSummary").html('&nbsp;' + jqXHR.statusText + ' (<u>Expand</u>)');
                    var $frame = $('#errorMessageDetail');
                    var doc = $frame.get(0).contentWindow.document;
                    doc.write(jqXHR.responseText);
                    self.fatalErrorMessage(true);
                }
                */

                /*
                    Bad request
                */
                if (jqXHR.status === 400) {
                    console.log(jqXHR.responseText);
                    json = jqXHR.responseJSON;

                    if (json.validation_errors) {
                        
                        var errz = json.validation_errors;
                        for(var field in errz) {
                            $('[name=' + field + ']')
                                .parent().addClass('has-error')
                                .find('> .error').text(errz[field]);
                        }
                        
                    }
                }
                else if (jqXHR.status === 500) {
                    console.log(jqXHR.responseText);
                    self.errorMessage(jqXHR.responseText);
                }
            },
            success : function(data, textStatus, jqXHR) {
                // Not Loading ...
                self.loading.hide();
                $('.has-error').removeClass('has-error').find('> .error').text('');

                if (data) {
                    console.log(data);
                }

                self.setSuccessFlashMessage(method);

                if (opts.successCallback) {
                    opts.successCallback(data, textStatus, jqXHR);
                    return;
                }
            }
        });
    },    
    // ----------------------------------------------------------------

    getCookie : function(name) {
        match = document.cookie.match(new RegExp(name + '=([^;]+)'));
        if (match) return match[1];
    },
    // ----------------------------------------------------------------

    setSuccessFlashMessage: function(method) {
        var self = this;

        if (method === 'PUT') {
            self.setSuccessMessage('Updated.');
        }
        if (method == 'POST') {
            self.setSuccessMessage('Created.');
        }
        if (method == 'DELETE') {
            self.setSuccessMessage('Deleted.');
        }
    },

    // ----------------------------------------------------------------
    /*
        Set success message on AJAX action via browser storage.
        The system will read the value on every page load, display,
        and delete the message.
    */
    setSuccessMessage : function(message) {
        if (Modernizr.localstorage == false) {
            console.log('Browser storage unavailable');
            return;
        }

        localStorage.setItem("message.success", message);
    },
    // ----------------------------------------------------------------
    /*
        Read success message.
    */
    getSuccessMessage : function() {
        var self = this;

        if (Modernizr.localstorage == false) {
            console.log('Browser storage unavailable');
            return;
        }

        message = localStorage.getItem("message.success");
        if (message) {
            self.successMessage(message);
            localStorage.removeItem('message.success');

        }
    },
    // ----------------------------------------------------------------

});
