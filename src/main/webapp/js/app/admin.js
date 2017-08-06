function MetadataField(data) {
  this.id = data ? data.id : null;
  this.name = data ? ko.observable(data.name) : ko.observable();
  this.type = data ? ko.observable(data.field_type) : ko.observable();
}


AdminViewModel = BaseViewModel.extend({
  constructor : function() {
    "use strict";

    var self = this;

    this.base();
    console.log('Initializing admin view model');

    self.metadataFields = ko.observableArray();
    self.getMetadataFields();
  },

  showAddMetadataField: function() {
    var self = this;
    self.fireEvent('showModalDialog', {detail: {modalId: '#addMetadataFieldModal'}});

    var modalEl = $('#addMetadataFieldModal');

    $('#addMetadataFieldModal-name').val('');

    modalEl.off('shown.bs.modal');
    modalEl.on('shown.bs.modal', function () {
      $('#addMetadataFieldModal-name').focus().select();
    });

    modalEl.modal();
  },

  addMetadataField: function() {
    var self = this;

    var opts = {
      'data': {
        'name': $('#addMetadataFieldModal-name').val(),
        'type': $('#addMetadataFieldModal-type').val()
      },
      errorContainerId: 'addMetadataFieldModal-form',
      'successCallback': function() {
        $('#addMetadataFieldModal').modal('hide');
        self.getMetadataFields();
      }
    };

    self.post('/api/v1/admin/metadata', opts);
  },

  getMetadataFields: function() {
    var self = this;

    var opts = {
      'successCallback': function(json) {
        var fields = $.map(json['fields'], function(data) {
          return new MetadataField(data);
        });

        self.metadataFields(fields);
      }
    };

    self.get('/api/v1/admin/metadata', opts);
  }
});
