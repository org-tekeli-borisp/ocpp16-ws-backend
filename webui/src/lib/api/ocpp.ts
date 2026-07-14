import type { ChargePoint, OcppMessage, CommandName, Transaction } from '$lib/types';

const API_BASE = '/api/chargepoints';

export async function fetchChargePoints(): Promise<ChargePoint[]> {
  const res = await fetch(API_BASE);
  if (!res.ok) throw new Error(`HTTP ${res.status}`);
  return res.json();
}

export async function fetchChargePoint(cpId: string): Promise<ChargePoint> {
  const res = await fetch(`${API_BASE}/${encodeURIComponent(cpId)}`);
  if (!res.ok) throw new Error(`HTTP ${res.status}`);
  return res.json();
}

export async function fetchCommands(cpId: string): Promise<CommandName[]> {
  const res = await fetch(`${API_BASE}/${encodeURIComponent(cpId)}/commands`);
  if (!res.ok) throw new Error(`HTTP ${res.status}`);
  return res.json();
}

export async function sendCommand(
  cpId: string,
  command: CommandName,
  payload: Record<string, unknown>,
): Promise<CommandResponse> {
  const res = await fetch(
    `${API_BASE}/${encodeURIComponent(cpId)}/commands/${command}`,
    {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload),
    },
  );
  const body = await res.text();
  return { status: res.status, statusText: res.statusText, body };
}

export interface MessageFilter {
  direction?: 'INBOUND' | 'OUTBOUND';
  action?: string;
  limit?: number;
}

export async function fetchMessages(
  cpId: string,
  history: boolean,
  filter?: MessageFilter,
): Promise<OcppMessage[]> {
  const params = new URLSearchParams({
    limit: String(filter?.limit ?? 200),
  });
  if (filter?.direction) params.set('direction', filter.direction);
  if (filter?.action) params.set('action', filter.action);

  const path = history ? 'messages/history' : 'messages';
  const res = await fetch(
    `${API_BASE}/${encodeURIComponent(cpId)}/${path}?${params}`,
  );
  if (!res.ok) throw new Error(`HTTP ${res.status}`);
  const data = await res.json();
  return history && data.messages ? data.messages : data;
}

export async function fetchTransactions(
  cpId: string,
  running = false,
): Promise<Transaction[]> {
  const params = running ? '?running=true' : '';
  const res = await fetch(
    `${API_BASE}/${encodeURIComponent(cpId)}/transactions${params}`,
  );
  if (!res.ok) throw new Error(`HTTP ${res.status}`);
  return res.json();
}
