<script lang="ts">
  import type { ChargePoint } from '$lib/types';
  import { t } from '$lib/i18n';
  import StationItem from '$components/StationItem.svelte';
  import { chargePoints, selectedCpId, searchQuery } from '$stores/app';

  export let stations: ChargePoint[];

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
