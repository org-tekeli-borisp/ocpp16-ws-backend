import { describe, it, expect } from 'vitest';
import { COMMAND_DEFINITIONS } from '$lib/commands';
import type { CommandDefinition } from '$lib/types';

describe('COMMAND_DEFINITIONS', () => {
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
    const def = COMMAND_DEFINITIONS['reset'];
    expect(def.fields).toHaveLength(1);
    expect(def.fields[0].name).toBe('type');
    expect(def.fields[0].options).toEqual(['Hard', 'Soft']);
  });

  it('clear-cache has no fields', () => {
    expect(COMMAND_DEFINITIONS['clear-cache'].fields).toHaveLength(0);
  });

  it('unlock-connector requires connectorId', () => {
    const def = COMMAND_DEFINITIONS['unlock-connector'];
    expect(def.fields).toHaveLength(1);
    expect(def.fields[0].required).toBe(true);
  });
});
