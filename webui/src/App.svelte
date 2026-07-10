<script lang="ts">
  import './app.css';
  import { locale, t } from '$lib/i18n';
  import { fetchChargePoints, fetchChargePoint, fetchCommands } from '$lib/api/ocpp';
  import { chargePoints, selectedCpId, activeTab, commandsCache } from '$stores/app';
  import type { ChargePoint, TabKey, CommandName } from '$lib/types';
  import Sidebar from '$components/Sidebar.svelte';
  import OverviewPanel from '$components/OverviewPanel.svelte';
  import CommandsPanel from '$components/CommandsPanel.svelte';
  import MessagesPanel from '$components/MessagesPanel.svelte';

  let error: string | null = null;
  let currentCp: ChargePoint | null = null;
  let currentCommands: CommandName[] = [];
  let refreshTimer: ReturnType<typeof setInterval> | null = null;

  $: badgeCp = $selectedCpId ? ($chargePoints.find(c => c.chargePointId === $selectedCpId) || currentCp) : null;

  async function initData() {
    try {
      const stations = await fetchChargePoints();
      chargePoints.set(stations);

      const params = new URLSearchParams(window.location.search);
      const cpFromUrl = params.get('cp');
      const tabFromHash = window.location.hash.slice(1) as TabKey;

      if (tabFromHash && ['overview', 'commands', 'messages'].includes(tabFromHash)) {
        activeTab.set(tabFromHash);
      }

      if (cpFromUrl) {
        await selectStation(cpFromUrl);
      }
    } catch (err) {
      error = (err as Error).message;
    }
  }

  async function selectStation(cpId: string) {
    selectedCpId.set(cpId);
    updateUrl(cpId, $activeTab);
    try {
      currentCp = await fetchChargePoint(cpId);
      const list = $chargePoints;
      const idx = list.findIndex(c => c.chargePointId === cpId);
      if (idx >= 0) {
        list[idx] = currentCp;
        chargePoints.set([...list]);
      }
      currentCommands = await fetchCommands(cpId);
      const cache = $commandsCache;
      cache[cpId] = currentCommands;
      commandsCache.set({ ...cache });
    } catch (err) {
      error = (err as Error).message;
    }
  }

  function updateUrl(cpId: string, tab: TabKey) {
    const url = new URL(window.location);
    url.searchParams.set('cp', cpId);
    url.hash = tab;
    window.history.replaceState({}, '', url);
  }

  function handleTabChange(tab: TabKey) {
    activeTab.set(tab);
    if ($selectedCpId) updateUrl($selectedCpId, tab);
  }

  $: if ($selectedCpId) {
    if (refreshTimer) clearInterval(refreshTimer);
    refreshTimer = setInterval(async () => {
      try {
        const updated = await fetchChargePoint($selectedCpId);
        currentCp = updated;
        const list = $chargePoints;
        const idx = list.findIndex(c => c.chargePointId === $selectedCpId);
        if (idx >= 0) {
          list[idx] = updated;
          chargePoints.set([...list]);
        }
      } catch { /* ignore */ }
    }, 10000);
  } else {
    if (refreshTimer) { clearInterval(refreshTimer); refreshTimer = null; }
  }

  function handleLangChange(e: Event) {
    const lang = (e.target as HTMLSelectElement).value;
    locale.set(lang);
  }

  initData();
</script>

<header>
  <h1>{t('page_title')}</h1>
  <div class="header-right">
    {#if badgeCp}
      <span class="station-badge">
        <span class="status-dot {badgeCp.status === 'ONLINE' ? 'online' : 'offline'}"></span>
        {badgeCp.chargePointId}
      </span>
    {/if}
    <span style="opacity:.4">|</span>
    <select class="lang-select" onchange={handleLangChange}>
      <option value="de">DE</option>
      <option value="en">EN</option>
      <option value="fr">FR</option>
    </select>
  </div>
</header>

<div class="layout">
  <Sidebar stations={$chargePoints} />

  <main class="main">
    <div class="tabs">
      <button class="tab {$activeTab === 'overview' ? 'active' : ''}" onclick={() => handleTabChange('overview')}>
        {t('tab_overview')}
      </button>
      <button class="tab {$activeTab === 'commands' ? 'active' : ''}" onclick={() => handleTabChange('commands')}>
        {t('tab_commands')}
      </button>
      <button class="tab {$activeTab === 'messages' ? 'active' : ''}" onclick={() => handleTabChange('messages')}>
        {t('tab_messages')}
      </button>
    </div>

    <div class="content">
      {#if error}
        <div class="error-state">{t('error_loading')}: {error}</div>
      {:else if !$selectedCpId}
        <div class="no-selection">{t('select_station_hint')}</div>
      {:else if $activeTab === 'overview' && currentCp}
        <OverviewPanel chargePoint={currentCp} />
      {:else if $activeTab === 'commands'}
        <CommandsPanel cpId={$selectedCpId} commands={currentCommands} connectors={currentCp?.connectors || []} />
      {:else if $activeTab === 'messages'}
        <MessagesPanel cpId={$selectedCpId} />
      {/if}
    </div>
  </main>
</div>
