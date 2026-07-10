<script lang="ts">
  import type { ChargePoint } from '$lib/types';
  import { t } from '$lib/i18n';
  import ConnectorChip from '$components/ConnectorChip.svelte';

  export let chargePoint: ChargePoint;

  function formatDate(iso: string): string {
    if (!iso) return '–';
    const d = new Date(iso);
    const locale = document.documentElement.lang || 'de-DE';
    return d.toLocaleDateString(locale, { day:'2-digit', month:'2-digit', year:'numeric' })
      + ' ' + d.toLocaleTimeString(locale, { hour:'2-digit', minute:'2-digit' });
  }

  function timeAgo(iso: string): string {
    if (!iso) return '–';
    const diff = Date.now() - new Date(iso).getTime();
    const sec = Math.floor(diff / 1000);
    if (sec < 60) return t('just_now');
    const min = Math.floor(sec / 60);
    if (min < 60) return `${min} ${t('time_unit_min')}`;
    const h = Math.floor(min / 60);
    if (h < 24) return `${h} ${t('time_unit_hour')}`;
    return formatDate(iso);
  }
</script>

<div class="panel">
  <h2>{chargePoint.chargePointId}</h2>
  <div class="panel-body">
    <div class="overview-grid">
      <div class="info-card">
        <div class="info-label">{t('label_status')}</div>
        <div class="info-value">
          <span class="status-dot {chargePoint.status === 'ONLINE' ? 'online' : 'offline'}"></span>
          {chargePoint.status === 'ONLINE' ? t('label_online') : t('label_offline')}
        </div>
      </div>
      <div class="info-card">
        <div class="info-label">{t('label_vendor_model')}</div>
        <div class="info-value">{[chargePoint.vendor, chargePoint.model].filter(Boolean).join(' / ') || '–'}</div>
      </div>
      <div class="info-card">
        <div class="info-label">{t('label_firmware')}</div>
        <div class="info-value">{chargePoint.firmwareVersion || '–'}</div>
      </div>
      <div class="info-card">
        <div class="info-label">{t('label_connected_since')}</div>
        <div class="info-value">{formatDate(chargePoint.createdAt)}</div>
      </div>
      <div class="info-card">
        <div class="info-label">{t('label_last_seen')}</div>
        <div class="info-value">{timeAgo(chargePoint.lastSeenAt)}</div>
      </div>
    </div>
    <div style="margin-top:1.2rem;">
      <div class="info-label">{t('label_connectors')}</div>
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
