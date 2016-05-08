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
    this.errorStacktrace = ko.observable();

    /*
     Shared folder browser functionality
     */
    this.directoryNames = ko.observableArray();
    this.currentPath = ko.observable();
    this.disableDoubleClick = false;

  },
  // ----------------------------------------------------------------

  blinkSuccess: function(msg) {
    this.successEl.show();
    this.successMessage(msg);
    this.successEl.fadeOut(1500);
  },
  // ----------------------------------------------------------------

  blinkInfo: function(msg) {
    this.infoEl.show();
    this.infoMessage(msg);
    this.infoEl.fadeOut(1800);
  },
  // ----------------------------------------------------------------

  blinkWarning: function(msg) {
    this.warningEl.show();
    this.warningMessage(msg);
    this.warningEl.fadeOut(1800);
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

    $.ajax({
      type : method,
      url : url,
      crossDomain : true,
      dataType: 'json',
      contentType: "application/json",
      data : JSON.stringify(data),
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
              console.log("error selector:", selector);
              $(selector)
                  .parent().addClass('has-error').parent()
                  .find('.error').text(errz[field]);
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

        console.log(typeof  opts.successCallback);
        if (opts.successCallback) {
          console.log(opts.successCallback);
          opts.successCallback(json, textStatus, jqXHR);
        }

        console.log(opts);
        if (opts.finally) {
          console.log('here');
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
  },

  /*
    Shared folder browser functionality
   */

  getDirectoryNames: function(path) {
    var self = this;
    var opts = {
      'successCallback': function (json) {
        self.directoryNames(json.directory_names);
        self.currentPath(json.current_path);
      },
      'finally': function() {
        self.disableDoubleClick = false;
      }
    };

    if (path) {
      opts.data = {'path': path}
    }

    // some browsers can fire the doubleclick event when populating the list
    this.disableDoubleClick = true;
    this.get('/import/source/local/listing', opts);
  },

  gotoPreviousDirectory: function() {
    var self = this;
    var opts = {
      'successCallback': function (json) {
        self.directoryNames(json.directory_names);
        self.currentPath(json.current_path);
      },
      'finally': function() {
        self.disableDoubleClick = false;
      },
      'data': {
        'path': self.currentPath()}
    };

    // some browsers can fire the doubleclick event when populating the list
    this.disableDoubleClick = true;
    this.get('/import/source/local/listing/parent', opts);
  },

  dropIntoDirectory: function(directoryName) {
    if (this.disableDoubleClick == true) {
      return;
    }

    var dirSeparator = this.currentPath() == "/" ? '': '/';
    this.getDirectoryNames(this.currentPath() + dirSeparator + directoryName);
  },

  selectImportDirectory: function() {
    var directoryName = $('#importDirectoryList').val() || '';
    var dirSeparator = this.currentPath() == "/" ? '': '/';
    var importDirectoryPath = this.currentPath() + dirSeparator + directoryName;
    window.location = "/client/import?importDirectoryPath=" + importDirectoryPath;
  }

});
