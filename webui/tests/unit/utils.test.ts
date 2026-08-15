import { describe, it, expect, vi, beforeEach } from 'vitest';

describe('formatDuration', () => {
  let formatDuration: (seconds: number | null) => string;

  beforeEach(async () => {
    const mod = await import('$lib/utils');
    formatDuration = mod.formatDuration;
  });

  it('returns dash for null', () => {
    expect(formatDuration(null)).toBe('–');
  });

  it('returns dash for negative', () => {
    expect(formatDuration(-1)).toBe('–');
  });

  it('formats seconds', () => {
    expect(formatDuration(45)).toBe('45s');
  });

  it('formats 0 seconds', () => {
    expect(formatDuration(0)).toBe('0s');
  });

  it('formats minutes and seconds', () => {
    expect(formatDuration(125)).toBe('2m 5s');
  });

  it('formats exact minutes', () => {
    expect(formatDuration(180)).toBe('3m 0s');
  });

  it('formats hours and minutes', () => {
    expect(formatDuration(7200)).toBe('2h 0m');
  });

  it('formats hours and minutes with remainder', () => {
    expect(formatDuration(7350)).toBe('2h 2m');
  });

  it('formats 59m 59s as minutes', () => {
    expect(formatDuration(3599)).toBe('59m 59s');
  });

  it('formats 60m as 1h', () => {
    expect(formatDuration(3600)).toBe('1h 0m');
  });
});

describe('formatEnergy', () => {
  let formatEnergy: (wh: number | null) => string;

  beforeEach(async () => {
    const mod = await import('$lib/utils');
    formatEnergy = mod.formatEnergy;
  });

  it('returns dash for null', () => {
    expect(formatEnergy(null)).toBe('–');
  });

  it('returns dash for zero', () => {
    expect(formatEnergy(0)).toBe('–');
  });

  it('returns dash for negative', () => {
    expect(formatEnergy(-100)).toBe('–');
  });

  it('converts Wh to kWh with 2 decimals', () => {
    expect(formatEnergy(5000)).toBe('5.00 kWh');
  });

  it('converts with rounding', () => {
    expect(formatEnergy(1234)).toBe('1.23 kWh');
  });

  it('handles large values', () => {
    expect(formatEnergy(50000)).toBe('50.00 kWh');
  });
});

describe('formatDateTime', () => {
  let formatDateTime: (iso: string, loc: string) => string;

  beforeEach(async () => {
    const mod = await import('$lib/utils');
    formatDateTime = mod.formatDateTime;
  });

  it('formats with German locale', () => {
    const result = formatDateTime('2024-06-15T14:30:00Z', 'de');
    expect(result).toContain('15.');
    expect(result).toMatch(/\d{2}:\d{2}/);
  });

  it('formats with English locale', () => {
    const result = formatDateTime('2024-06-15T14:30:00Z', 'en');
    expect(result).toBeTruthy();
  });

  it('formats with French locale', () => {
    const result = formatDateTime('2024-06-15T14:30:00Z', 'fr');
    expect(result).toBeTruthy();
  });

  it('falls back to German for unknown locale', () => {
    const result = formatDateTime('2024-06-15T14:30:00Z', 'xx');
    expect(result).toBeTruthy();
  });
});

describe('getActiveDuration', () => {
  let getActiveDuration: (tx: { startTime: string }) => string;

  beforeEach(async () => {
    const mod = await import('$lib/utils');
    getActiveDuration = mod.getActiveDuration;
  });

  it('returns reasonable duration for recent start time', () => {
    const start = new Date(Date.now() - 300000).toISOString();
    const result = getActiveDuration({ startTime: start });
    expect(result).toContain('5m');
  });

  it('returns 0s for just-now start time', () => {
    const start = new Date().toISOString();
    const result = getActiveDuration({ startTime: start });
    expect(result).toContain('0s');
  });
});

describe('formatDate', () => {
  let formatDate: (iso: string, loc: string) => string;

  beforeEach(async () => {
    const mod = await import('$lib/utils');
    formatDate = mod.formatDate;
  });

  it('returns dash for empty string', () => {
    expect(formatDate('', 'de')).toBe('–');
  });

  it('returns dash for undefined cast to string', () => {
    expect(formatDate('undefined', 'de')).toBeTruthy();
  });

  it('formats with German locale', () => {
    const result = formatDate('2024-06-15T14:30:00Z', 'de');
    expect(result).toContain('15.');
  });

  it('formats with English locale', () => {
    const result = formatDate('2024-06-15T14:30:00Z', 'en');
    expect(result).toBeTruthy();
  });

  it('includes time portion', () => {
    const result = formatDate('2024-06-15T14:30:00Z', 'de');
    expect(result).toMatch(/\d{2}:\d{2}$/);
  });
});

describe('timeAgo', () => {
  let timeAgo: (iso: string, t: (key: string) => string) => string;

  beforeEach(async () => {
    const mod = await import('$lib/utils');
    timeAgo = mod.timeAgo;
  });

  const mockT = (key: string) => {
    const map: Record<string, string> = {
      just_now: 'just now',
      time_unit_min: 'min ago',
      time_unit_hour: 'hours ago',
    };
    return map[key] ?? key;
  };

  it('returns dash for empty string', () => {
    expect(timeAgo('', mockT)).toBe('–');
  });

  it('returns just_now for < 60 seconds', () => {
    const recent = new Date(Date.now() - 30000).toISOString();
    expect(timeAgo(recent, mockT)).toContain('just now');
  });

  it('returns minutes for < 60 minutes', () => {
    const ago = new Date(Date.now() - 1800000).toISOString();
    expect(timeAgo(ago, mockT)).toContain('30 min ago');
  });

  it('returns hours for < 24 hours', () => {
    const ago = new Date(Date.now() - 7200000).toISOString();
    expect(timeAgo(ago, mockT)).toContain('2 hours ago');
  });

  it('returns full date for > 24 hours', () => {
    const ago = new Date(Date.now() - 86400000 * 2).toISOString();
    const result = timeAgo(ago, mockT);
    expect(result).toContain('20');
  });
});

describe('getConnectorClass', () => {
  let getConnectorClass: (status: string) => string;

  beforeEach(async () => {
    const mod = await import('$lib/utils');
    getConnectorClass = mod.getConnectorClass;
  });

  it('returns avail for Available', () => {
    expect(getConnectorClass('Available')).toBe('avail');
  });

  it('returns avail for Preparing', () => {
    expect(getConnectorClass('Preparing')).toBe('avail');
  });

  it('returns charg for Charging', () => {
    expect(getConnectorClass('Charging')).toBe('charg');
  });

  it('returns fault for Faulted', () => {
    expect(getConnectorClass('Faulted')).toBe('fault');
  });

  it('returns unavail for Unavailable', () => {
    expect(getConnectorClass('Unavailable')).toBe('unavail');
  });

  it('returns other for SuspendedEVSE', () => {
    expect(getConnectorClass('SuspendedEVSE')).toBe('other');
  });

  it('returns other for SuspendedEV', () => {
    expect(getConnectorClass('SuspendedEV')).toBe('other');
  });

  it('returns other for Reserved', () => {
    expect(getConnectorClass('Reserved')).toBe('other');
  });

  it('returns other for Finishing', () => {
    expect(getConnectorClass('Finishing')).toBe('other');
  });

  it('returns other for unknown status', () => {
    expect(getConnectorClass('UnknownStatus')).toBe('other');
  });

  it('is case insensitive', () => {
    expect(getConnectorClass('available')).toBe('avail');
    expect(getConnectorClass('CHARGING')).toBe('charg');
    expect(getConnectorClass('Faulted')).toBe('fault');
  });
});

describe('filterMessages', () => {
  let filterMessages: (
    messages: Array<{ direction: string; action?: string }>,
    direction?: string,
    action?: string,
  ) => Array<{ direction: string; action?: string }>;

  beforeEach(async () => {
    const mod = await import('$lib/utils');
    filterMessages = mod.filterMessages;
  });

  it('returns all when no filters', () => {
    const msgs = [
      { direction: 'INBOUND', action: 'BootNotification' },
      { direction: 'OUTBOUND', action: 'reset' },
    ];
    expect(filterMessages(msgs)).toEqual(msgs);
  });

  it('filters by direction', () => {
    const msgs = [
      { direction: 'INBOUND', action: 'BootNotification' },
      { direction: 'OUTBOUND', action: 'reset' },
      { direction: 'INBOUND', action: 'Heartbeat' },
    ];
    const result = filterMessages(msgs, 'INBOUND', '');
    expect(result).toHaveLength(2);
    expect(result.every(m => m.direction === 'INBOUND')).toBe(true);
  });

  it('filters by action (case insensitive)', () => {
    const msgs = [
      { direction: 'INBOUND', action: 'BootNotification' },
      { direction: 'OUTBOUND', action: 'reset' },
      { direction: 'INBOUND', action: 'Heartbeat' },
    ];
    const result = filterMessages(msgs, '', 'boot');
    expect(result).toHaveLength(1);
    expect(result[0]?.action).toBe('BootNotification');
  });

  it('filters by both direction and action', () => {
    const msgs = [
      { direction: 'INBOUND', action: 'BootNotification' },
      { direction: 'OUTBOUND', action: 'reset' },
      { direction: 'INBOUND', action: 'Heartbeat' },
    ];
    const result = filterMessages(msgs, 'INBOUND', 'boot');
    expect(result).toHaveLength(1);
  });

  it('returns empty when no match', () => {
    const msgs = [
      { direction: 'INBOUND', action: 'BootNotification' },
    ];
    expect(filterMessages(msgs, 'OUTBOUND', '')).toHaveLength(0);
  });

  it('handles missing action field', () => {
    const msgs = [
      { direction: 'INBOUND' },
      { direction: 'INBOUND', action: 'Heartbeat' },
    ];
    const result = filterMessages(msgs, '', 'Heartbeat');
    expect(result).toHaveLength(1);
  });
});

describe('buildStationUrl', () => {
  let buildStationUrl: (base: string, cpId: string, tab: string) => string;

  beforeEach(async () => {
    const mod = await import('$lib/utils');
    buildStationUrl = mod.buildStationUrl;
  });

  it('sets cp parameter and tab hash on a clean base url', () => {
    expect(buildStationUrl('http://localhost:8080/webui/', 'CP-001', 'commands'))
      .toBe('http://localhost:8080/webui/?cp=CP-001#commands');
  });

  it('replaces an existing cp parameter and preserves other parameters', () => {
    expect(buildStationUrl('http://localhost:8080/webui/?cp=OLD&x=1', 'CP-001', 'messages'))
      .toBe('http://localhost:8080/webui/?cp=CP-001&x=1#messages');
  });

  it('replaces an existing hash', () => {
    expect(buildStationUrl('http://localhost:8080/webui/?x=1#overview', 'CP-001', 'transactions'))
      .toBe('http://localhost:8080/webui/?x=1&cp=CP-001#transactions');
  });

  it('encodes the charge point id', () => {
    expect(buildStationUrl('http://localhost:8080/', 'CP/01', 'overview'))
      .toBe('http://localhost:8080/?cp=CP%2F01#overview');
  });
});
