<script lang="ts">
  import type { OcppMessage } from '$lib/types';
  import { t } from '$lib/i18n';
  import { fetchMessages } from '$lib/api/ocpp';

  export let cpId: string;

  let messages: OcppMessage[] = [];
  let msgTab: 'live' | 'history' = 'live';
  let filterDirection: string = '';
  let filterAction: string = '';
  let refreshTimer: ReturnType<typeof setInterval> | null = null;

  $: messageCount = messages.length;

  async function loadMessages() {
    if (!cpId) return;
    try {
      messages = await fetchMessages(cpId, msgTab === 'history', {
        direction: filterDirection as 'INBOUND' | 'OUTBOUND' | undefined,
        action: filterAction || undefined,
        limit: 200,
      });
    } catch {
      messages = [];
    }
  }

  function switchTab(tab: 'live' | 'history') {
    msgTab = tab;
    loadMessages();
  }

  function applyFilters() {
    loadMessages();
  }

  function formatTime(iso: string): string {
    const lang = document.documentElement.lang || 'de-DE';
    return new Date(iso).toLocaleTimeString(lang, { hour:'2-digit', minute:'2-digit', second:'2-digit' });
  }

  function escapeHtml(s: string): string {
    const d = document.createElement('div');
    d.textContent = s;
    return d.innerHTML;
  }

  function escapeAttr(s: string): string {
    return s.replace(/"/g, '&quot;').replace(/'/g, '&#39;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
  }

  $: if (msgTab === 'live' && cpId) {
    if (refreshTimer) clearInterval(refreshTimer);
    refreshTimer = setInterval(loadMessages, 3000);
  } else {
    if (refreshTimer) { clearInterval(refreshTimer); refreshTimer = null; }
  }
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
              title={escapeAttr(msg.payload || '')}
            >{escapeHtml(msg.payload ? (msg.payload.length > 300 ? msg.payload.substring(0, 300) + '…' : msg.payload) : '–')}</span>
            <span class="msg-time">{formatTime(msg.timestamp)}</span>
          </div>
        {/each}
      {/if}
    </div>
  </div>
</div>
