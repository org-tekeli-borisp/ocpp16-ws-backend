<script lang="ts">
  import { onDestroy } from 'svelte';
  import type { Transaction } from '$lib/types';
  import { t } from '$lib/i18n';
  import { fetchTransactions } from '$lib/api/ocpp';

  export let cpId: string;

  let txTab: 'active' | 'history' = 'active';
  let activeTransactions: Transaction[] = [];
  let historyTransactions: Transaction[] = [];
  let loading = true;
  let filterConnector = '';
  let refreshTimer: ReturnType<typeof setInterval> | null = null;

  $: activeFiltered = activeTransactions.filter(tx =>
    filterConnector ? tx.connectorId === parseInt(filterConnector) : true
  );

  $: historyFiltered = historyTransactions.filter(tx =>
    filterConnector ? tx.connectorId === parseInt(filterConnector) : true
  );

  $: connectorIds = [...new Set(
    [...activeTransactions, ...historyTransactions].map(tx => tx.connectorId)
  )].sort((a, b) => a - b);

  async function loadAll() {
    loading = true;
    try {
      [activeTransactions, historyTransactions] = await Promise.all([
        fetchTransactions(cpId, true),
        fetchTransactions(cpId, false),
      ]);
    } catch {
      activeTransactions = [];
      historyTransactions = [];
    } finally {
      loading = false;
    }
  }

  function switchTab(tab: 'active' | 'history') {
    txTab = tab;
  }

  function formatDateTime(iso: string): string {
    const d = new Date(iso);
    const lang = document.documentElement.lang || 'de-DE';
    return d.toLocaleString(lang, {
      day: '2-digit',
      month: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
    });
  }

  function formatDuration(seconds: number | null): string {
    if (seconds == null || seconds < 0) return '–';
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

  function formatEnergy(wh: number | null): string {
    if (wh == null || wh <= 0) return '–';
    const kWh = wh / 1000;
    return `${kWh.toFixed(2)} kWh`;
  }

  function getActiveDuration(transaction: Transaction): string {
    const elapsed = Math.floor((Date.now() - new Date(transaction.startTime).getTime()) / 1000);
    return formatDuration(elapsed);
  }

  function getActiveEnergy(transaction: Transaction): string {
    return `${(transaction.meterStart / 1000).toFixed(2)} kWh`;
  }

  $: if (cpId) {
    if (refreshTimer) clearInterval(refreshTimer);
    loadAll();
    refreshTimer = setInterval(loadAll, 15000);
  } else {
    if (refreshTimer) { clearInterval(refreshTimer); refreshTimer = null; }
  }

  onDestroy(() => {
    if (refreshTimer) clearInterval(refreshTimer);
  });
</script>

<div class="panel">
  <div class="tx-header">
    <div class="tx-tabs">
      <button class="tx-tab {txTab === 'active' ? 'active' : ''}" onclick={() => switchTab('active')}>
        <span class="tx-tab-dot" style="display: {txTab === 'active' && activeFiltered.length > 0 ? 'inline-block' : 'none'}"></span>
        {$t('tx_tab_active')}
        {#if activeFiltered.length > 0}
          <span class="tx-badge">{activeFiltered.length}</span>
        {/if}
      </button>
      <button class="tx-tab {txTab === 'history' ? 'active' : ''}" onclick={() => switchTab('history')}>
        {$t('tx_tab_history')}
      </button>
    </div>
    <div class="tx-filters">
      <label style="font-size:.78rem;font-weight:600;">{$t('tx_filter_connector')}:</label>
      <select bind:value={filterConnector}>
        <option value="">{$t('tx_filter_all_connectors')}</option>
        {#each connectorIds as id}
          <option value={id}>{id}</option>
        {/each}
      </select>
      <button class="btn btn-sm btn-outline" onclick={loadAll}>{$t('tx_btn_refresh')}</button>
    </div>
  </div>

  <div class="panel-body">
    {#if loading}
      <p class="loading">{$t('tx_loading')}</p>
    {:else if txTab === 'active'}
      {#if activeFiltered.length === 0}
        <p class="empty-state">{$t('tx_no_active')}</p>
      {:else}
        <div class="tx-table-wrap">
          <table class="tx-table">
            <thead>
              <tr>
                <th>{$t('tx_connector')}</th>
                <th>{$t('tx_id_tag')}</th>
                <th>{$t('tx_start')}</th>
                <th>{$t('tx_energy')}</th>
                <th>{$t('tx_duration')}</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {#each activeFiltered as tx}
                <tr class="tx-row-active">
                  <td class="tx-conn">{tx.connectorId}</td>
                  <td class="tx-idtag">{tx.idTag}</td>
                  <td class="tx-time">{formatDateTime(tx.startTime)}</td>
                  <td class="tx-energy">{getActiveEnergy(tx)}</td>
                  <td class="tx-duration">
                    <span class="tx-live">
                      <span class="live-dot"></span>
                      {getActiveDuration(tx)}
                    </span>
                  </td>
                  <td class="tx-status">
                    <span class="tx-running-badge">{$t('tx_running')}</span>
                  </td>
                </tr>
              {/each}
            </tbody>
          </table>
        </div>
      {/if}
    {:else}
      {#if historyFiltered.length === 0}
        <p class="empty-state">{$t('tx_no_history')}</p>
      {:else}
        <div class="tx-table-wrap">
          <table class="tx-table">
            <thead>
              <tr>
                <th>{$t('tx_connector')}</th>
                <th>{$t('tx_id_tag')}</th>
                <th>{$t('tx_start')}</th>
                <th>{$t('tx_stop')}</th>
                <th>{$t('tx_energy')}</th>
                <th>{$t('tx_duration')}</th>
                <th>{$t('tx_stop_reason')}</th>
              </tr>
            </thead>
            <tbody>
              {#each historyFiltered as tx}
                <tr>
                  <td class="tx-conn">{tx.connectorId}</td>
                  <td class="tx-idtag">{tx.idTag}</td>
                  <td class="tx-time">{formatDateTime(tx.startTime)}</td>
                  <td class="tx-time">{tx.stopTime ? formatDateTime(tx.stopTime) : '–'}</td>
                  <td class="tx-energy">{formatEnergy(tx.energyWh)}</td>
                  <td class="tx-duration">{formatDuration(tx.durationSeconds)}</td>
                  <td class="tx-reason">{tx.stopReason || '–'}</td>
                </tr>
              {/each}
            </tbody>
          </table>
        </div>
      {/if}
    {/if}
  </div>
</div>
