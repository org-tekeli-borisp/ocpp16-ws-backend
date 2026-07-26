import type { CommandDefinition } from '$lib/types';

export const COMMAND_DEFINITIONS: Record<string, CommandDefinition> = {
  'reset': {
    labelKey: 'cmd_reset',
    fields: [
      { name: 'type', labelKey: 'field_type', type: 'select', required: true, options: ['Hard', 'Soft'] },
    ],
  },
  'clear-cache': { labelKey: 'cmd_clear_cache', fields: [] },
  'get-local-list-version': { labelKey: 'cmd_get_local_list_version', fields: [] },
  'unlock-connector': {
    labelKey: 'cmd_unlock_connector',
    fields: [{ name: 'connectorId', labelKey: 'field_connector_id', type: 'number', required: true }],
  },
  'remote-start-transaction': {
    labelKey: 'cmd_remote_start_transaction',
    fields: [
      { name: 'idTag', labelKey: 'field_id_tag', type: 'text', required: true },
      { name: 'connectorId', labelKey: 'field_connector_id', type: 'number', required: true },
    ],
  },
  'remote-stop-transaction': {
    labelKey: 'cmd_remote_stop_transaction',
    fields: [{ name: 'transactionId', labelKey: 'field_transaction_id', type: 'number', required: true }],
  },
  'change-availability': {
    labelKey: 'cmd_change_availability',
    fields: [
      { name: 'connectorId', labelKey: 'field_connector_id', type: 'number', required: true },
      { name: 'type', labelKey: 'field_type', type: 'select', required: true, options: ['Inoperative', 'Operative'] },
    ],
  },
  'change-configuration': {
    labelKey: 'cmd_change_configuration',
    fields: [
      { name: 'key', labelKey: 'field_key', type: 'text', required: true },
      { name: 'value', labelKey: 'field_value', type: 'text', required: true },
    ],
  },
  'trigger-message': {
    labelKey: 'cmd_trigger_message',
    fields: [
      { name: 'requestedMessage', labelKey: 'field_requested_message', type: 'select', required: true,
    options: ['BootNotification', 'DiagnosticsStatusNotification', 'FirmwareStatusNotification',
              'Heartbeat', 'MeterValues', 'StatusNotification'] },
      { name: 'connectorId', labelKey: 'field_connector_id', type: 'number', required: false },
    ],
  },
  'extended-trigger-message': {
    labelKey: 'cmd_extended_trigger_message',
    fields: [
      { name: 'requestedMessage', labelKey: 'field_requested_message', type: 'select', required: true,
        options: ['BootNotification', 'LogStatusNotification', 'FirmwareStatusNotification',
                  'Heartbeat', 'MeterValues', 'SignChargePointCertificate', 'StatusNotification'] },
      { name: 'connectorId', labelKey: 'field_connector_id', type: 'number', required: false },
    ],
  },
  'cancel-reservation': {
    labelKey: 'cmd_cancel_reservation',
    fields: [{ name: 'reservationId', labelKey: 'field_reservation_id', type: 'number', required: true }],
  },
  'get-composite-schedule': {
    labelKey: 'cmd_get_composite_schedule',
    fields: [
      { name: 'connectorId', labelKey: 'field_connector_id', type: 'number', required: true },
      { name: 'duration', labelKey: 'field_duration', type: 'number', required: true },
    ],
  },
  'get-configuration': {
    labelKey: 'cmd_get_configuration',
    fields: [{ name: 'key', labelKey: 'field_keys_hint', type: 'text', required: false }],
  },
  'get-diagnostics': {
    labelKey: 'cmd_get_diagnostics',
    fields: [
      { name: 'protocol', labelKey: 'field_upload_protocol', type: 'radio', required: false, options: ['ftp', 'sftp'], defaultValue: 'ftp' },
      { name: 'location', labelKey: 'field_upload_url', type: 'text', required: false, hintKey: 'field_upload_url_hint' },
      { name: 'retries', labelKey: 'field_retries', type: 'number', required: false },
      { name: 'retryInterval', labelKey: 'field_retry_interval', type: 'number', required: false },
    ],
  },
  'reserve-now': {
    labelKey: 'cmd_reserve_now',
    fields: [
      { name: 'connectorId', labelKey: 'field_connector_id', type: 'number', required: true },
      { name: 'reservationId', labelKey: 'field_reservation_id', type: 'number', required: true },
      { name: 'expiryDate', labelKey: 'field_expiry_date', type: 'text', required: true },
      { name: 'idTag', labelKey: 'field_id_tag', type: 'text', required: true },
    ],
  },
  'send-local-list': {
    labelKey: 'cmd_send_local_list',
    fields: [
      { name: 'listVersion', labelKey: 'field_list_version', type: 'number', required: true },
      { name: 'updateType', labelKey: 'field_update_type', type: 'select', required: true, options: ['Differential', 'Full'] },
    ],
  },
  'update-firmware': {
    labelKey: 'cmd_update_firmware',
    fields: [
      { name: 'location', labelKey: 'field_firmware_url', type: 'text', required: true },
      { name: 'retrieveDate', labelKey: 'field_retrieve_date', type: 'text', required: true },
    ],
  },
  'clear-charging-profile': {
    labelKey: 'cmd_clear_charging_profile',
    fields: [
      { name: 'connectorId', labelKey: 'field_connector_id', type: 'number', required: false },
      { name: 'stackLevel', labelKey: 'field_stack_level', type: 'number', required: false },
    ],
  },
  'get-installed-certificate-ids': {
    labelKey: 'cmd_get_installed_cert_ids',
    fields: [{ name: 'certificateType', labelKey: 'field_cert_type', type: 'select', required: true,
               options: ['CentralSystemRootCertificate', 'ManufacturerRootCertificate'] }],
  },
  'install-certificate': {
    labelKey: 'cmd_install_certificate',
    fields: [
      { name: 'certificateType', labelKey: 'field_cert_type', type: 'select', required: true,
        options: ['CentralSystemRootCertificate', 'ManufacturerRootCertificate'] },
      { name: 'certificate', labelKey: 'field_certificate_pem', type: 'textarea', required: true, hintKey: 'field_cert_max' },
    ],
  },
  'delete-certificate': {
    labelKey: 'cmd_delete_certificate',
    fields: [{ name: 'certificateHashData', labelKey: 'field_cert_hash', type: 'json', required: true, hintKey: 'field_cert_hash_hint' }],
  },
  'get-log': {
    labelKey: 'cmd_get_log',
    fields: [
      { name: 'logType', labelKey: 'field_log_type', type: 'select', required: true, options: ['DiagnosticsLog', 'SecurityLog'] },
      { name: 'requestId', labelKey: 'field_request_id', type: 'number', required: true },
      { name: 'log', labelKey: 'field_log_params', type: 'json', required: true, hintKey: 'field_log_hint' },
      { name: 'retries', labelKey: 'field_retries', type: 'number', required: false },
      { name: 'retryInterval', labelKey: 'field_retry_interval', type: 'number', required: false },
    ],
  },
  'set-charging-profile': {
    labelKey: 'cmd_set_charging_profile',
    fields: [
      { name: 'connectorId', labelKey: 'field_connector_id', type: 'number', required: true },
      { name: 'chargingProfileId', labelKey: 'field_charging_profile_id', type: 'number', required: true },
      { name: 'stackLevel', labelKey: 'field_stack_level', type: 'number', required: false, defaultValue: '0' },
      { name: 'chargingProfilePurpose', labelKey: 'field_charging_profile_purpose', type: 'select', required: true,
        options: ['ChargePointMaxProfile', 'TxProfile', 'TxDefaultProfile'] },
      { name: 'chargingProfileKind', labelKey: 'field_charging_profile_kind', type: 'select', required: true,
        options: ['Absolute', 'Relative'] },
      { name: 'duration', labelKey: 'field_duration_seconds', type: 'number', required: false },
      { name: 'chargingRateUnit', labelKey: 'field_charging_rate_unit', type: 'select', required: true,
        options: ['A', 'W'] },
      { name: 'limit', labelKey: 'field_charging_limit', type: 'number', required: true },
    ],
  },
  'signed-update-firmware': {
    labelKey: 'cmd_signed_update_firmware',
    fields: [
      { name: 'requestId', labelKey: 'field_request_id', type: 'number', required: true },
      { name: 'firmware', labelKey: 'field_firmware_json', type: 'json', required: true, hintKey: 'field_firmware_json_hint' },
      { name: 'retries', labelKey: 'field_retries', type: 'number', required: false },
      { name: 'retryInterval', labelKey: 'field_retry_interval', type: 'number', required: false },
    ],
  },
  'data-transfer': {
    labelKey: 'cmd_data_transfer',
    fields: [
      { name: 'vendorId', labelKey: 'field_vendor_id', type: 'text', required: true },
      { name: 'messageId', labelKey: 'field_message_id', type: 'text', required: false },
      { name: 'data', labelKey: 'field_data', type: 'text', required: false },
    ],
  },
  'send-certificate-signed': {
    labelKey: 'cmd_send_certificate_signed',
    fields: [
      { name: 'certificateChain', labelKey: 'field_certificate_chain', type: 'textarea', required: true, hintKey: 'field_cert_chain_hint' },
    ],
  },
};
