jQuery.support.cors = true;

if (!window.console) {
  console = {
    log : function() {}
  };
}

ko.bindingHandlers.click = {
  init: function(element, valueAccessor, allBindingsAccessor, viewModel, context) {
    var accessor = valueAccessor();
    var clicks = 0;
    var timeout = 200;

    $(element).click(function(event) {
      if(typeof(accessor) === 'object') {
        var single = accessor.single;
        var double = accessor.double;
        clicks++;
        if (clicks === 1) {
          setTimeout(function() {
            if(clicks === 1) {
              single.call(viewModel, context.$data, event);
            } else {
              double.call(viewModel, context.$data, event);
            }
            clicks = 0;
          }, timeout);
        }
      } else {
        accessor.call(viewModel, context.$data, event);
      }
    });
  }
};

BaseViewModel = Base.extend({
  constructor : function() {
    var self = this;
    console.log('Initializing base view model');
    this.base();

    this.successMessage = ko.observable();
    this.successEl = $('#successMessage');

    this.infoMessage = ko.observable();
    this.infoEl = $('#infoMessage');

    this.warningMessage = ko.observable();
    this.warningEl = $('#warningMessage');

    this.errorMessage = ko.observable();
    this.errorStacktrace = ko.observable();

    this.keys = [];
    window.onkeyup = function(e) {self.keys[e.keyCode]=false;};
    window.onkeydown = function(e) {self.keys[e.keyCode]=true;}
  },

  isKeyPressed: function(key) {
    return this.keys.has(key);
  },
  // ----------------------------------------------------------------

  success: function(msg) {
    this.successMessage(msg);
  },
  // ----------------------------------------------------------------

  info: function(msg) {
    this.infoMessage(msg);
  },
  // ----------------------------------------------------------------

  warning: function(msg) {
    this.warningMessage(msg);
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
    $(el).find('.has-error').removeClass('has-error').parent().find('.error').text('');
  },
  // ----------------------------------------------------------------

  resetAllMessages: function() {
    this.successMessage(null);
    this.infoMessage(null);
    this.warningMessage(null);
    this.errorMessage(null);
    this.errorStacktrace(null);
    this.dismissError();
  },
  // ----------------------------------------------------------------

  hide: function(el) {
    $(el).hide();
  },
  // ----------------------------------------------------------------

  restRequest : function(url, method, opts) {
    var self = this;

    self.resetAllMessages();

    method = method.toUpperCase();

    data = opts.data || {};

    var contentType = 'application/x-www-form-urlencoded; charset=UTF-8';
    if (method === 'POST' || method === 'PUT') {
      contentType = "application/json";
      data = JSON.stringify(data);
    }

    $.ajax({
      type : method,
      url : url,
      crossDomain : true,
      dataType: 'json', // expected
      contentType: contentType, // sent
      data : data,
      cache : false,
      error : function(jqXHR, textStatus, errorThrown) {
        // unhandled exceptions
        if (jqXHR.status === 500) {
          console.log(jqXHR.responseText);
          var msg = jqXHR.responseJSON ? jqXHR.responseJSON.error : jqXHR.responseText;
          var stacktrace = jqXHR.responseJSON ? jqXHR.responseJSON.stacktrace : jqXHR.responseText;
          self.showError();
          self.errorMessage(msg);
          self.errorStacktrace(stacktrace);
        }

        // validation errors
        if (jqXHR.status === 400) {
          var json = jqXHR.responseJSON;

          // if there is a container where we want to look for errors, we must specify it
          if (json.validation_errors) {
            var errz = json.validation_errors;
            var errElId = '#' + opts.errorContainerId;

            for(var field in errz) {
              var errSelector = '[name=' + field + ']';
              var selector = errElId ? errElId + ' ' + errSelector : errSelector;
              var errEl = $(selector).parent().addClass('has-error').parent().find('.error');
              console.log(errEl);
              errEl.text(errz[field]);
              errEl.css('display', 'inline');
            }
          }
          else if (json.validation_error) {
            self.showError();
            self.errorMessage(json.validation_error);
          }
          else if (json.error) {
            self.showError();
            self.errorMessage(json.error);
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
  },

  reportError: function() {
    console.log("error report placeholder");
  },

  showError: function() {
    $('#errorContainer').show();
  },

  dismissError: function() {
    $('#errorContainer').hide();
  }
});
