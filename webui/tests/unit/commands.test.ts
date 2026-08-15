import { describe, it, expect } from 'vitest';
import { COMMAND_DEFINITIONS } from '$lib/commands';
import type { CommandDefinition } from '$lib/types';

describe('COMMAND_DEFINITIONS', () => {
  function def(cmd: string): CommandDefinition {
    const d = COMMAND_DEFINITIONS[cmd];
    if (!d) throw new Error(`missing command definition: ${cmd}`);
    return d;
  }

  it('has definitions for all expected commands', () => {
    const expected = [
      'reset', 'clear-cache', 'get-local-list-version', 'unlock-connector',
      'remote-start-transaction', 'remote-stop-transaction', 'change-availability',
      'change-configuration', 'trigger-message', 'extended-trigger-message',
      'cancel-reservation', 'get-composite-schedule', 'get-configuration',
      'get-diagnostics', 'reserve-now', 'send-local-list', 'update-firmware',
      'clear-charging-profile', 'get-installed-certificate-ids',
      'install-certificate', 'delete-certificate', 'get-log',
      'set-charging-profile', 'signed-update-firmware',
      'data-transfer', 'send-certificate-signed',
    ];
    for (const cmd of expected) {
      expect(COMMAND_DEFINITIONS[cmd]).toBeDefined();
      expect(COMMAND_DEFINITIONS[cmd]).toMatchObject<CommandDefinition>({
        labelKey: expect.any(String),
        fields: expect.any(Array),
      });
    }
  });

  it('reset command has Hard/Soft options', () => {
    expect(def('reset').fields).toHaveLength(1);
    expect(def('reset').fields[0]?.name).toBe('type');
    expect(def('reset').fields[0]?.options).toEqual(['Hard', 'Soft']);
  });

  it('clear-cache has no fields', () => {
    expect(def('clear-cache').fields).toHaveLength(0);
  });

  it('unlock-connector requires connectorId', () => {
    expect(def('unlock-connector').fields).toHaveLength(1);
    expect(def('unlock-connector').fields[0]?.required).toBe(true);
  });

  it('data-transfer has vendorId required field', () => {
    const fields = def('data-transfer').fields;
    expect(fields.length).toBeGreaterThanOrEqual(1);
    const vendorId = fields.find(f => f.name === 'vendorId');
    expect(vendorId).toBeDefined();
    expect(vendorId!.required).toBe(true);
  });

  it('send-certificate-signed has certificateChain required field', () => {
    const fields = def('send-certificate-signed').fields;
    expect(fields.length).toBeGreaterThanOrEqual(1);
    const certChain = fields.find(f => f.name === 'certificateChain');
    expect(certChain).toBeDefined();
    expect(certChain!.required).toBe(true);
  });
});
