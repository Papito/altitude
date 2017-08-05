AdminViewModel = BaseViewModel.extend({
  constructor : function() {
    "use strict";

    var self = this;

    this.base();
    console.log('Initializing admin view model');
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
      }
    };

    self.post('/api/v1/admin/metadata', opts);

  }
});
