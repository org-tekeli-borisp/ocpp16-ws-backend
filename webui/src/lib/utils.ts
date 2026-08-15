const LANG_MAP: Record<string, string> = { de: 'de-DE', en: 'en-US', fr: 'fr-FR' };

function getLang(loc: string): string {
  return LANG_MAP[loc] || 'de-DE';
}

export function formatDuration(seconds: number | null): string {
  if (seconds == null || seconds < 0) return '\u2013';
  if (seconds < 60) return `${seconds}s`;
  if (seconds < 3600) {
    const m = Math.floor(seconds / 60);
    const s = seconds % 60;
    return `${m}m ${s}s`;
  }
  const h = Math.floor(seconds / 3600);
  const m = Math.floor((seconds % 3600) / 60);
  return `${h}h ${m}m`;
}

export function formatEnergy(wh: number | null): string {
  if (wh == null || wh <= 0) return '\u2013';
  const kWh = wh / 1000;
  return `${kWh.toFixed(2)} kWh`;
}

export function formatDateTime(iso: string, loc: string): string {
  const d = new Date(iso);
  const lang = getLang(loc);
  return d.toLocaleString(lang, {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
}

export function getActiveDuration(transaction: { startTime: string }): string {
  const elapsed = Math.floor((Date.now() - new Date(transaction.startTime).getTime()) / 1000);
  return formatDuration(elapsed);
}

export function formatDate(iso: string, loc: string): string {
  if (!iso) return '\u2013';
  const d = new Date(iso);
  const lang = getLang(loc);
  return d.toLocaleDateString(lang, { day: '2-digit', month: '2-digit', year: 'numeric' })
    + ' ' + d.toLocaleTimeString(lang, { hour: '2-digit', minute: '2-digit' });
}

export function timeAgo(iso: string, t: (key: string) => string): string {
  if (!iso) return '\u2013';
  const diff = Date.now() - new Date(iso).getTime();
  const sec = Math.floor(diff / 1000);
  if (sec < 60) return t('just_now');
  const min = Math.floor(sec / 60);
  if (min < 60) return `${min} ${t('time_unit_min')}`;
  const h = Math.floor(min / 60);
  if (h < 24) return `${h} ${t('time_unit_hour')}`;
  return formatDate(iso, 'de');
}

export function getConnectorClass(status: string): string {
  const s = status.toLowerCase();
  if (s === 'available' || s === 'preparing') return 'avail';
  if (s === 'charging') return 'charg';
  if (s === 'faulted') return 'fault';
  if (s === 'unavailable') return 'unavail';
  return 'other';
}

export function filterMessages<T extends { direction: string; action?: string }>(
  messages: T[],
  direction?: string,
  action?: string,
): T[] {
  return messages.filter(msg => {
    if (direction && msg.direction !== direction) return false;
    if (action && !(msg.action || '').toLowerCase().includes(action.toLowerCase())) return false;
    return true;
  });
}

export function formatBytes(bytes: number): string {
  if (bytes <= 0) return '0 B';
  const units = ['B', 'KB', 'MB', 'GB'];
  const i = Math.floor(Math.log(bytes) / Math.log(1024));
  const size = bytes / Math.pow(1024, i);
  return `${size.toFixed(i > 0 ? 1 : 0)} ${units[i]}`;
}

export function formatDateTimeDefault(iso: string): string {
  return formatDateTime(iso, 'de');
}

export function buildStationUrl(base: string, cpId: string, tab: string): string {
  const url = new URL(base);
  url.searchParams.set('cp', cpId);
  url.hash = tab;
  return url.href;
}
