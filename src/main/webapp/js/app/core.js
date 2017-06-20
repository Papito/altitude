jQuery.support.cors = true;

if (!window.console) {
  console = {
    log : function() {}
  };
}


function assert(condition, message) {
  if (!condition) {
    message = message || "Assertion failed";
    if (typeof Error !== "undefined") {
      throw new Error(message);
    }
    throw message; // Fallback
  }
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

    this.keys = [];

    // for finding which key is currently pressed
    window.onkeyup = function(e) {self.keys[e.keyCode]=false;};
    window.onkeydown = function(e) {self.keys[e.keyCode]=true;};

    document.addEventListener('requestMade', function (data) {
      self.resetAllMessages();
    }, false);

    document.addEventListener('showInlineDialog', function (data) {
      console.log('Handling: showInlineDialog');
      self.resetAllMessages();
    }, false);

    document.addEventListener('showModalDialog', function (data) {
      console.log('Handling: showModalDialog');
      self.resetAllMessages();
    }, false);

    document.addEventListener('successMsg', function (data) {
      $.toast({
        heading: 'Success',
        text: data.detail.msg,
        showHideTransition: 'fade',
        icon: 'success',
        loader: false,
        position: 'bottom-right',
        stack: 1
      });
    }, false);

    document.addEventListener('infoMsg', function (data) {
      $.toast({
        heading: 'Info',
        text: data.detail.msg,
        showHideTransition: 'fade',
        icon: 'info',
        loader: false,
        position: 'bottom-right',
        stack: 1

      });
    }, false);

    document.addEventListener('warningMsg', function (data) {
      $.toast({
        heading: 'Warning',
        text: data.detail.msg,
        showHideTransition: 'fade',
        icon: 'warning',
        loader: false,
        position: 'bottom-right',
        stack: 1

      });
    }, false);

    this.stacktraceMessage = ko.observable();
    this.stacktraceDetail = ko.observable();
    this.stacktraceCount = 0; // to avoid applying the bindings
    document.addEventListener('stacktrace', function (data) {
      self.stacktraceMessage(data.detail.msg);
      self.stacktraceDetail(data.detail.stacktrace);

      $.toast({
        heading: 'Critical Error!',
        text: '<a id="stacktraceDetailsLink" href="#" data-bind="click: function(){$root.showStacktraceDetailModal()}">CLICK HERE</a> to review error details',
        showHideTransition: 'fade',
        hideAfter: false,
        icon: 'error',
        loader: false,
        position: 'top-left',
        stack: 1
      });

      if (self.stacktraceCount === 0) {
        ko.applyBindings(new BaseViewModel(), $('#stacktraceDetailsLink').get(0));
      }

      self.stacktraceCount++;

    }, false);
  },

  isKeyPressed: function(key) {
    return this.keys.has(key);
  },
  // ----------------------------------------------------------------

  success: function(msg) {
    this.fireEvent('successMsg', {detail: {msg: msg}});
  },
  // ----------------------------------------------------------------

  info: function(msg) {
    this.fireEvent('infoMsg', {detail: {msg: msg}});
  },
  // ----------------------------------------------------------------

  warning: function(msg) {
    this.fireEvent('warningMsg', {detail: {msg: msg}});
  },
  // ----------------------------------------------------------------

  stacktrace: function(msg, stacktrace) {
    this.fireEvent('stacktrace', {detail: {msg: msg, stacktrace: stacktrace}});
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

  showStacktraceDetailModal: function() {
    var self = this;
    console.log(self.stacktraceDetail());
    self.fireEvent('showModalDialog');
    self.fireEvent('showStacktraceModalDialog');
    $('#stacktraceModal').modal()
  },

  get : function(url, opts) {
    return this.restRequest(url, 'GET', opts);
  },
  // ----------------------------------------------------------------

  submitStacktrace: function() {

  },
  // ----------------------------------------------------------------

  resetFormErrors: function(el) {
    $(el).find('.has-error').removeClass('has-error').parent().find('.error').text('');
  },
  // ----------------------------------------------------------------

  resetAllMessages: function() {
    $.toast().reset('all');
  },
  // ----------------------------------------------------------------

  hide: function(el) {
    $(el).hide();
  },
  // ----------------------------------------------------------------

  restRequest : function(url, method, opts) {
    var self = this;

    self.fireEvent('requestMade', {url: url, method: method});

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
          self.stacktrace(msg, stacktrace);
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

  fireEvent: function(eventName, data) {
    console.log('Firing event', eventName, data);
    var e = new CustomEvent(eventName, data);
    document.dispatchEvent(e);
  }
});
