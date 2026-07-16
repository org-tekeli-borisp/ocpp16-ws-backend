import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { get } from 'svelte/store';

describe('stores/app', () => {
  beforeEach(() => {
    vi.resetModules();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  describe('chargePoints', () => {
    it('initializes as empty array', async () => {
      const { chargePoints } = await import('$stores/app');
      expect(get(chargePoints)).toEqual([]);
    });

    it('is writable with charge points', async () => {
      const { chargePoints } = await import('$stores/app');
      const points = [
        { chargePointId: 'CP-001', status: 'ONLINE' as const, connectors: [], createdAt: '', lastSeenAt: '' },
        { chargePointId: 'CP-002', status: 'OFFLINE' as const, connectors: [], createdAt: '', lastSeenAt: '' },
      ];
      chargePoints.set(points);
      expect(get(chargePoints)).toEqual(points);
      expect(get(chargePoints)).toHaveLength(2);
    });

    it('supports update', async () => {
      const { chargePoints } = await import('$stores/app');
      chargePoints.update(pts => [
        { chargePointId: 'CP-001', status: 'ONLINE' as const, connectors: [], createdAt: '', lastSeenAt: '' },
        ...pts,
      ]);
      expect(get(chargePoints)).toHaveLength(1);
    });
  });

  describe('selectedCpId', () => {
    it('initializes as null', async () => {
      const { selectedCpId } = await import('$stores/app');
      expect(get(selectedCpId)).toBeNull();
    });

    it('is writable with a charge point ID', async () => {
      const { selectedCpId } = await import('$stores/app');
      selectedCpId.set('CP-001');
      expect(get(selectedCpId)).toBe('CP-001');
    });

    it('can be cleared back to null', async () => {
      const { selectedCpId } = await import('$stores/app');
      selectedCpId.set('CP-001');
      selectedCpId.set(null);
      expect(get(selectedCpId)).toBeNull();
    });
  });

  describe('activeTab', () => {
    it('initializes as overview', async () => {
      const { activeTab } = await import('$stores/app');
      expect(get(activeTab)).toBe('overview');
    });

    it('is writable with any TabKey', async () => {
      const { activeTab } = await import('$stores/app');
      activeTab.set('commands');
      expect(get(activeTab)).toBe('commands');

      activeTab.set('messages');
      expect(get(activeTab)).toBe('messages');

      activeTab.set('transactions');
      expect(get(activeTab)).toBe('transactions');
    });
  });

  describe('commandsCache', () => {
    it('initializes as empty object', async () => {
      const { commandsCache } = await import('$stores/app');
      expect(get(commandsCache)).toEqual({});
    });

    it('is writable with cache entries', async () => {
      const { commandsCache } = await import('$stores/app');
      commandsCache.set({
        'CP-001': ['reset', 'clear-cache'],
        'CP-002': ['trigger-message'],
      });
      const cache = get(commandsCache);
      expect(Object.keys(cache)).toHaveLength(2);
      expect(cache['CP-001']).toContain('reset');
    });

    it('supports update to add entries', async () => {
      const { commandsCache } = await import('$stores/app');
      commandsCache.update(cache => ({
        ...cache,
        'CP-003': ['unlock-connector'],
      }));
      const cache = get(commandsCache);
      expect(cache['CP-003']).toEqual(['unlock-connector']);
    });
  });

  describe('searchQuery', () => {
    it('initializes as empty string', async () => {
      const { searchQuery } = await import('$stores/app');
      expect(get(searchQuery)).toBe('');
    });

    it('is writable with search text', async () => {
      const { searchQuery } = await import('$stores/app');
      searchQuery.set('CP-0');
      expect(get(searchQuery)).toBe('CP-0');
    });

    it('can be cleared', async () => {
      const { searchQuery } = await import('$stores/app');
      searchQuery.set('some query');
      searchQuery.set('');
      expect(get(searchQuery)).toBe('');
    });
  });
});
