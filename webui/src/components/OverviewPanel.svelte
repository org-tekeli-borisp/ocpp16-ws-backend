<script lang="ts">
import type { ChargePoint } from '$lib/types';
import { t, locale } from '$lib/i18n';
import ConnectorChip from '$components/ConnectorChip.svelte';
import { formatDate, timeAgo } from '$lib/utils';
import { disconnectChargePoint } from '$lib/api/ocpp';

  export let chargePoint: ChargePoint;
  export let onRefresh: (() => Promise<void>) | null = null;

  let disconnecting = false;

  async function handleDisconnect() {
    if (!$t('disconnect_confirm')) return;
    if (!confirm($t('disconnect_confirm'))) return;
    disconnecting = true;
    try {
      await disconnectChargePoint(chargePoint.chargePointId);
      await onRefresh?.();
    } catch (err) {
      alert($t('network_error') + ': ' + (err as Error).message);
    } finally {
      disconnecting = false;
    }
  }
</script>

<div class="panel">
  <h2>
    <span>{chargePoint.chargePointId}</span>
    {#if onRefresh}
      <div class="panel-header-right">
        {#if chargePoint.status === 'ONLINE'}
          <button class="btn btn-sm btn-danger" onclick={handleDisconnect} disabled={disconnecting}>
            {disconnecting ? $t('btn_disconnecting') : $t('btn_disconnect')}
          </button>
        {/if}
        <button class="btn btn-sm btn-outline" onclick={onRefresh}>{$t('diag_btn_refresh')}</button>
      </div>
    {/if}
  </h2>
  <div class="panel-body">
    <div class="overview-grid">
      <div class="info-card">
        <div class="info-label">{$t('label_status')}</div>
        <div class="info-value">
          <span class="status-dot {chargePoint.status === 'ONLINE' ? 'online' : 'offline'}"></span>
          {chargePoint.status === 'ONLINE' ? $t('label_online') : $t('label_offline')}
        </div>
      </div>
      <div class="info-card">
        <div class="info-label">{$t('label_vendor_model')}</div>
        <div class="info-value">{[chargePoint.vendor, chargePoint.model].filter(Boolean).join(' / ') || '–'}</div>
      </div>
      <div class="info-card">
        <div class="info-label">{$t('label_firmware')}</div>
        <div class="info-value">{chargePoint.firmwareVersion || '–'}</div>
      </div>
      <div class="info-card">
        <div class="info-label">{$t('label_first_registered')}</div>
        <div class="info-value">{formatDate(chargePoint.createdAt, $locale)}</div>
      </div>
      <div class="info-card">
        <div class="info-label">{$t('label_last_connected')}</div>
        <div class="info-value">{timeAgo(chargePoint.lastConnectedAt, $t)}</div>
      </div>
      <div class="info-card">
        <div class="info-label">{$t('label_last_seen')}</div>
        <div class="info-value">{timeAgo(chargePoint.lastSeenAt, $t)}</div>
      </div>
    </div>
    <div style="margin-top:1.2rem;">
      <div class="info-label">{$t('label_connectors')}</div>
      <div class="connector-list">
        {#each chargePoint.connectors as conn}
          <ConnectorChip connector={conn} />
        {:else}
          <span style="color:#6c757d;font-size:.78rem;">–</span>
        {/each}
      </div>
    </div>
  </div>
</div>
