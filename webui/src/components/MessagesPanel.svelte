<script lang="ts">
  import { onMount } from 'svelte';
  import type { OcppMessage } from '$lib/types';
  import { t, locale } from '$lib/i18n';
  import { fetchMessages } from '$lib/api/ocpp';

  export let cpId: string;

  let allMessages: OcppMessage[] = [];
  let msgTab: 'live' | 'history' = 'live';
  let filterDirection: string = '';
  let filterAction: string = '';
  let refreshTimer: ReturnType<typeof setInterval> | null = null;

  $: messages = allMessages.filter(msg => {
    if (filterDirection && msg.direction !== filterDirection) return false;
    if (filterAction && !(msg.action || '').toLowerCase().includes(filterAction.toLowerCase())) return false;
    return true;
  });

  $: messageCount = messages.length;

  async function loadMessages() {
    if (!cpId) return;
    try {
      allMessages = await fetchMessages(cpId, msgTab === 'history', {
        limit: 200,
      });
    } catch {
      allMessages = [];
    }
  }

  function switchTab(tab: 'live' | 'history') {
    msgTab = tab;
    loadMessages();
  }

  function applyFilters() {
    // Client-side filtering — no-op, reactive $: handles it
  }

  function formatTime(iso: string, loc: string): string {
    const langMap: Record<string, string> = { de: 'de-DE', en: 'en-US', fr: 'fr-FR' };
    const lang = langMap[loc] || 'de-DE';
    return new Date(iso).toLocaleString(lang, { day:'2-digit', month:'2-digit', year:'numeric', hour:'2-digit', minute:'2-digit', second:'2-digit' });
  }



  $: if (msgTab === 'live' && cpId) {
    if (refreshTimer) clearInterval(refreshTimer);
    refreshTimer = setInterval(loadMessages, 3000);
    loadMessages();
  } else {
    if (refreshTimer) { clearInterval(refreshTimer); refreshTimer = null; }
  }

  onMount(loadMessages);
</script>

<div class="panel">
  <h2>{$t('label_messages')}</h2>
  <div class="panel-body">
    <div class="msg-tabs">
      <button class="msg-tab {msgTab === 'live' ? 'active' : ''}" onclick={() => switchTab('live')}>Live</button>
      <button class="msg-tab {msgTab === 'history' ? 'active' : ''}" onclick={() => switchTab('history')}>Historie</button>
    </div>

    <div class="filters">
      <label style="font-size:.78rem;font-weight:600;">{$t('filter_direction')}:</label>
      <select bind:value={filterDirection}>
        <option value="">{$t('filter_all')}</option>
        <option value="INBOUND">C→S {$t('filter_inbound')}</option>
        <option value="OUTBOUND">S→C {$t('filter_outbound')}</option>
      </select>
      <label style="font-size:.78rem;font-weight:600;">{$t('filter_action')}:</label>
      <input type="text" bind:value={filterAction} placeholder="z.B. Heartbeat" />
      <button class="btn btn-sm btn-outline" onclick={applyFilters}>{$t('btn_apply')}</button>
      <div class="status-bar" style="margin-left:auto;">
        <span>{messageCount} {$t('label_messages_lower')}</span>
        <span class="live-indicator" style="display:{msgTab === 'live' ? 'inline-flex' : 'none'}">
          <span class="live-dot"></span> {$t('label_live')}
        </span>
      </div>
    </div>

    <div class="message-list">
      {#if messages.length === 0}
        <p class="empty-state">{$t('no_messages')}</p>
      {:else}
        {#each messages as msg}
          <div class="message-item">
            <span class="msg-direction {msg.direction === 'INBOUND' ? 'inbound' : 'outbound'}">
              {msg.direction === 'INBOUND' ? 'C→S' : 'S→C'}
            </span>
            <span class="msg-type">{msg.messageType}</span>
            <span class="msg-action">{msg.action || '–'}</span>
            <span
              class="msg-payload"
              title={msg.payload || ''}
            >{msg.payload || '–'}</span>
            <span class="msg-time">{formatTime(msg.timestamp, $locale)}</span>
          </div>
        {/each}
      {/if}
    </div>
  </div>
</div>
