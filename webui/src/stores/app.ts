import { writable, type Writable } from 'svelte/store';
import type { ChargePoint, CommandName, TabKey } from '$lib/types';

export const chargePoints: Writable<ChargePoint[]> = writable([]);
export const selectedCpId: Writable<string | null> = writable(null);
export const activeTab: Writable<TabKey> = writable('overview');
export const commandsCache: Writable<Record<string, CommandName[]>> = writable({});
export const searchQuery: Writable<string> = writable('');
