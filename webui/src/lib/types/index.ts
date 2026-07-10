export type ConnectorStatus =
  | 'Available'
  | 'Preparing'
  | 'Charging'
  | 'SuspendedEVSE'
  | 'SuspendedEV'
  | 'Finishing'
  | 'Reserved'
  | 'Unavailable'
  | 'Faulted';

export interface Connector {
  connectorId: number;
  status: ConnectorStatus;
}

export interface ChargePoint {
  chargePointId: string;
  vendor?: string;
  model?: string;
  firmwareVersion?: string;
  status: 'ONLINE' | 'OFFLINE';
  connectors: Connector[];
  createdAt: string;
  lastSeenAt: string;
}

export interface OcppMessage {
  timestamp: string;
  direction: 'INBOUND' | 'OUTBOUND';
  messageType: string;
  action?: string;
  payload?: string;
}

export type CommandName =
  | 'reset'
  | 'clear-cache'
  | 'get-local-list-version'
  | 'unlock-connector'
  | 'remote-start-transaction'
  | 'remote-stop-transaction'
  | 'change-availability'
  | 'change-configuration'
  | 'trigger-message'
  | 'extended-trigger-message'
  | 'cancel-reservation'
  | 'get-composite-schedule'
  | 'get-configuration'
  | 'get-diagnostics'
  | 'reserve-now'
  | 'send-local-list'
  | 'update-firmware'
  | 'clear-charging-profile'
  | 'get-installed-certificate-ids'
  | 'install-certificate'
  | 'delete-certificate'
  | 'get-log'
  | 'set-charging-profile'
  | 'signed-update-firmware';

export interface CommandField {
  name: string;
  labelKey: string;
  type: 'text' | 'number' | 'select' | 'textarea' | 'json';
  required: boolean;
  options?: string[];
  hintKey?: string;
}

export interface CommandDefinition {
  labelKey: string;
  fields: CommandField[];
}

export interface CommandResponse {
  status: number;
  statusText: string;
  body: string;
}

export type TabKey = 'overview' | 'commands' | 'messages';
