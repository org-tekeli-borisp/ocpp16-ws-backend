<script lang="ts">
  import type { ChargePoint } from '$lib/types';
  import { t } from '$lib/i18n';
  import StationItem from '$components/StationItem.svelte';
  import { chargePoints, selectedCpId, searchQuery } from '$stores/app';
  import { reconnectAllChargePoints } from '$lib/api/ocpp';

  export let stations: ChargePoint[];
  export let onSelectStation: ((cpId: string) => void) | null = null;
  export let onRefresh: (() => Promise<void>) | null = null;

  let disconnecting = false;

  async function handleDisconnectAll() {
    if (onlineCount === 0) return;
    if (!confirm($t('disconnect_all_confirm'))) return;
    disconnecting = true;
    try {
      await reconnectAllChargePoints();
      await onRefresh?.();
    } catch (err) {
      alert($t('network_error') + ': ' + (err as Error).message);
    } finally {
      disconnecting = false;
    }
  }

  $: filtered = stations.filter(cp => {
    if (!$searchQuery) return true;
    const q = $searchQuery.toLowerCase();
    return cp.chargePointId.toLowerCase().includes(q)
      || (cp.vendor || '').toLowerCase().includes(q)
      || (cp.model || '').toLowerCase().includes(q);
  });

  $: onlineCount = stations.filter(cp => cp.status === 'ONLINE').length;
  $: offlineCount = stations.length - onlineCount;

  function handleSelect(cpId: string) {
    selectedCpId.set(cpId);
    onSelectStation?.(cpId);
  }
</script>

<div class="sidebar">
  <div class="sidebar-header">
    <h2>{$t('label_stations')}</h2>
    <div class="sidebar-stats">
      <span class="online-c">{onlineCount}</span> {$t('label_online')}
      <span class="offline-c">{offlineCount}</span> {$t('label_offline')}
    </div>
    <input
      type="text"
      class="search-box"
      placeholder={$t('search_placeholder')}
      bind:value={$searchQuery}
    />
    {#if onlineCount > 0}
      <button class="btn btn-sm btn-danger" onclick={handleDisconnectAll} disabled={disconnecting}>
        {disconnecting ? $t('btn_disconnecting') : $t('btn_disconnect_all')}
      </button>
    {/if}
  </div>
  <div class="sidebar-list">
    {#if filtered.length === 0}
      <div class="empty-state">{$t('no_stations')}</div>
    {:else}
      {#each filtered as station}
        <StationItem
          {station}
          isActive={station.chargePointId === $selectedCpId}
          onSelect={handleSelect}
        />
      {/each}
    {/if}
  </div>
</div>
