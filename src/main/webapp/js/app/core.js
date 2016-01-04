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

    this.successMessage = ko.observable();
    this.successEl = $('#successMessage');

    this.infoMessage = ko.observable();
    this.infoEl = $('#infoMessage');

    this.warningMessage = ko.observable();
    this.warningEl = $('#warningMessage');

    this.errorMessage = ko.observable();
    this.errorEl = $('#errorMessage');
  },
  // ----------------------------------------------------------------

  blinkSuccess: function(msg) {
    this.successEl.show();
    this.successMessage(msg);
    this.successEl.fadeOut(5000);
  },
  // ----------------------------------------------------------------

  blinkInfo: function(msg) {
    this.infoEl.show();
    this.infoMessage(msg);
    this.infoEl.fadeOut(5000);
  },
  // ----------------------------------------------------------------

  blinkWarning: function(msg) {
    this.warningEl.show();
    this.warningMessage(msg);
    this.warningEl.fadeOut(5000);
  },
  // ----------------------------------------------------------------

  blinkError: function(msg) {
    this.errorEl.show();
    this.errorMessage(msg);
    this.errorEl.fadeOut(5000);
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

  resetFormErrors: function(el) {
    $(el).find('.has-error').removeClass('has-error').parent().find('> .error').text('');
  },

  hide: function(el) {
    $(el).hide();
  },

  restRequest : function(url, method, opts) {
    method = method.toUpperCase();

    data = opts.data || {};

    $.ajax({
      type : method,
      url : url,
      crossDomain : true,
      data : data,
      cache : false,
      error : function(jqXHR, textStatus, errorThrown) {
        // unhandled exceptions
        if (jqXHR.status === 500) {
          console.log(jqXHR.responseText);
        }

        // validation errors
        if (jqXHR.status === 400) {
          var json = jqXHR.responseJSON;

          if (json.validationErrors) {
            var errz = json.validationErrors;
            for(var field in errz) {
              $('[name=' + field + ']')
                  .parent().addClass('has-error').parent()
                  .find('.error').text(errz[field]);
            }
          }
        }

        // if error container given, dump the message there
        if (opts.errorContainer) {
          var errJson = jqXHR.responseJSON;
          var errText = jqXHR.responseText;
          var text = errJson ? errJson.error : errText;
          $('#' + opts.errorContainer).text(text).show();
        }

        if (opts.errorCallback) {
          opts.errorCallback(jqXHR, textStatus, errorThrown);
        }

        if (opts.finally) {
          opts.finally();
        }

      },
      success : function(json, textStatus, jqXHR) {
        if (json) {
          console.log(json);
        }

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
