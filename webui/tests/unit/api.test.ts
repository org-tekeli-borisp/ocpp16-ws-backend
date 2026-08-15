import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import type { Mock } from 'vitest';
import type { OcppMessage, Transaction, CommandResponse } from '$lib/types';

const API_BASE = '/api/chargepoints';

function createFetchMock(response: unknown, status = 200) {
  return vi.fn().mockResolvedValue({
    ok: status < 400,
    status,
    statusText: status === 200 ? 'OK' : 'Error',
    json: vi.fn().mockResolvedValue(response),
    text: vi.fn().mockResolvedValue(typeof response === 'string' ? response : JSON.stringify(response)),
  });
}

describe('OCPP API Client', () => {
  beforeEach(() => {
    vi.resetModules();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  describe('fetchChargePoints', () => {
    it('returns charge points on success', async () => {
      const mockData = [
        { chargePointId: 'CP-001', status: 'ONLINE', connectors: [], createdAt: '2024-01-01', lastSeenAt: '2024-01-01' },
        { chargePointId: 'CP-002', status: 'OFFLINE', connectors: [], createdAt: '2024-01-02', lastSeenAt: '2024-01-02' },
      ];
      vi.stubGlobal('fetch', createFetchMock(mockData));

      const { fetchChargePoints } = await import('$lib/api/ocpp');
      const result = await fetchChargePoints();

      expect(result).toEqual(mockData);
      expect(fetch).toHaveBeenCalledWith(API_BASE);
    });

    it('throws on HTTP error', async () => {
      vi.stubGlobal('fetch', createFetchMock(null, 500));

      const { fetchChargePoints } = await import('$lib/api/ocpp');
      await expect(fetchChargePoints()).rejects.toThrow('HTTP 500');
    });
  });

  describe('fetchChargePoint', () => {
    it('returns single charge point on success', async () => {
      const mockData = {
        chargePointId: 'CP-001',
        vendor: 'Tesla',
        model: 'Model3',
        status: 'ONLINE',
        connectors: [{ connectorId: 1, status: 'Available' }],
        createdAt: '2024-01-01',
        lastSeenAt: '2024-01-01',
      };
      vi.stubGlobal('fetch', createFetchMock(mockData));

      const { fetchChargePoint } = await import('$lib/api/ocpp');
      const result = await fetchChargePoint('CP-001');

      expect(result).toEqual(mockData);
      expect(fetch).toHaveBeenCalledWith(`${API_BASE}/CP-001`);
    });

    it('encodes chargePointId in URL', async () => {
      const mockData = { chargePointId: 'CP/001', status: 'ONLINE', connectors: [], createdAt: '2024-01-01', lastSeenAt: '2024-01-01' };
      vi.stubGlobal('fetch', createFetchMock(mockData));

      const { fetchChargePoint } = await import('$lib/api/ocpp');
      await fetchChargePoint('CP/001');

      expect(fetch).toHaveBeenCalledWith(`${API_BASE}/CP%2F001`);
    });

    it('throws on HTTP error', async () => {
      vi.stubGlobal('fetch', createFetchMock(null, 404));

      const { fetchChargePoint } = await import('$lib/api/ocpp');
      await expect(fetchChargePoint('UNKNOWN')).rejects.toThrow('HTTP 404');
    });
  });

  describe('fetchCommands', () => {
    it('returns command names on success', async () => {
      const mockData = ['reset', 'clear-cache', 'trigger-message'];
      vi.stubGlobal('fetch', createFetchMock(mockData));

      const { fetchCommands } = await import('$lib/api/ocpp');
      const result = await fetchCommands('CP-001');

      expect(result).toEqual(mockData);
      expect(fetch).toHaveBeenCalledWith(`${API_BASE}/CP-001/commands`);
    });

    it('throws on HTTP error', async () => {
      vi.stubGlobal('fetch', createFetchMock(null, 500));

      const { fetchCommands } = await import('$lib/api/ocpp');
      await expect(fetchCommands('CP-001')).rejects.toThrow('HTTP 500');
    });
  });

  describe('sendCommand', () => {
    it('sends POST with correct payload', async () => {
      const responseText = '{"status":"Accepted"}';
      vi.stubGlobal('fetch', createFetchMock(responseText));

      const { sendCommand } = await import('$lib/api/ocpp');
      const result = await sendCommand('CP-001', 'reset', { type: 'Soft' });

      expect(fetch).toHaveBeenCalledWith(
        `${API_BASE}/CP-001/commands/reset`,
        {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ type: 'Soft' }),
        },
      );
      expect(result).toMatchObject({ status: 200, body: responseText });
    });

    it('returns CommandResponse with error status', async () => {
      vi.stubGlobal('fetch', createFetchMock('Bad Request', 400));

      const { sendCommand } = await import('$lib/api/ocpp');
      const result = await sendCommand('CP-001', 'reset', { type: 'Soft' });

      expect(result.status).toBe(400);
    });

    it('encodes chargePointId in URL', async () => {
      vi.stubGlobal('fetch', createFetchMock('{}'));

      const { sendCommand } = await import('$lib/api/ocpp');
      await sendCommand('CP/001', 'reset', {});

      const callUrl = (fetch as unknown as Mock).mock.calls[0]?.[0];
      expect(callUrl).toContain('CP%2F001');
    });
  });

  describe('fetchMessages', () => {
    it('returns messages for non-history mode', async () => {
      const mockMessages: OcppMessage[] = [
        { timestamp: '2024-01-01', direction: 'INBOUND', messageType: 'Call', action: 'BootNotification' },
      ];
      vi.stubGlobal('fetch', createFetchMock(mockMessages));

      const { fetchMessages } = await import('$lib/api/ocpp');
      const result = await fetchMessages('CP-001', false);

      expect(result).toEqual(mockMessages);
      const url = (fetch as unknown as Mock).mock.calls[0]?.[0];
      expect(url).toContain('/messages?');
      expect(url).toContain('limit=200');
    });

    it('unwraps data.messages for history mode', async () => {
      const mockMessages: OcppMessage[] = [
        { timestamp: '2024-01-01', direction: 'INBOUND', messageType: 'Call', action: 'Heartbeat' },
      ];
      vi.stubGlobal('fetch', createFetchMock({ messages: mockMessages, total: 1 }));

      const { fetchMessages } = await import('$lib/api/ocpp');
      const result = await fetchMessages('CP-001', true);

      expect(result).toEqual(mockMessages);
      const url = (fetch as unknown as Mock).mock.calls[0]?.[0];
      expect(url).toContain('/messages/history?');
    });

    it('applies direction filter', async () => {
      vi.stubGlobal('fetch', createFetchMock([]));

      const { fetchMessages } = await import('$lib/api/ocpp');
      await fetchMessages('CP-001', false, { direction: 'OUTBOUND' });

      const url = (fetch as unknown as Mock).mock.calls[0]?.[0];
      expect(url).toContain('direction=OUTBOUND');
    });

    it('applies action filter', async () => {
      vi.stubGlobal('fetch', createFetchMock([]));

      const { fetchMessages } = await import('$lib/api/ocpp');
      await fetchMessages('CP-001', false, { action: 'BootNotification' });

      const url = (fetch as unknown as Mock).mock.calls[0]?.[0];
      expect(url).toContain('action=BootNotification');
    });

    it('applies custom limit', async () => {
      vi.stubGlobal('fetch', createFetchMock([]));

      const { fetchMessages } = await import('$lib/api/ocpp');
      await fetchMessages('CP-001', false, { limit: 50 });

      const url = (fetch as unknown as Mock).mock.calls[0]?.[0];
      expect(url).toContain('limit=50');
    });

    it('throws on HTTP error', async () => {
      vi.stubGlobal('fetch', createFetchMock(null, 500));

      const { fetchMessages } = await import('$lib/api/ocpp');
      await expect(fetchMessages('CP-001', false)).rejects.toThrow('HTTP 500');
    });
  });

  describe('fetchTransactions', () => {
    it('returns all transactions by default', async () => {
      const mockTransactions: Transaction[] = [
        {
          id: 1, chargePointId: 'CP-001', connectorId: 1, idTag: 'CARD1',
          meterStart: 0, startTime: '2024-01-01', stopTime: '2024-01-01T01:00:00',
          meterStop: 5000, stopReason: 'Local', durationSeconds: 3600, energyWh: 5000,
        },
      ];
      vi.stubGlobal('fetch', createFetchMock(mockTransactions));

      const { fetchTransactions } = await import('$lib/api/ocpp');
      const result = await fetchTransactions('CP-001');

      expect(result).toEqual(mockTransactions);
      const url = (fetch as unknown as Mock).mock.calls[0]?.[0];
      expect(url).toBe(`${API_BASE}/CP-001/transactions`);
    });

    it('filters running transactions', async () => {
      vi.stubGlobal('fetch', createFetchMock([]));

      const { fetchTransactions } = await import('$lib/api/ocpp');
      await fetchTransactions('CP-001', true);

      const url = (fetch as unknown as Mock).mock.calls[0]?.[0];
      expect(url).toContain('?running=true');
    });

    it('throws on HTTP error', async () => {
      vi.stubGlobal('fetch', createFetchMock(null, 500));

      const { fetchTransactions } = await import('$lib/api/ocpp');
      await expect(fetchTransactions('CP-001')).rejects.toThrow('HTTP 500');
    });
  });
});
