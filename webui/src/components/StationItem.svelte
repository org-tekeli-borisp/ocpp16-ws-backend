<script lang="ts">
  import type { ChargePoint } from '$lib/types';
  import { t } from '$lib/i18n';

  export let station: ChargePoint;
  export let isActive: boolean;
  export let onSelect: (cpId: string) => void;

  function handleClick() {
    onSelect(station.chargePointId);
  }
</script>

<div
  class="station-item {isActive ? 'active' : ''}"
  onclick={handleClick}
  role="button"
  tabindex={0}
  aria-selected={isActive}
>
  <div>
    <div class="station-name">
      <span class="status-dot {station.status === 'ONLINE' ? 'online' : 'offline'}"></span>
      {station.chargePointId}
    </div>
    <div class="station-meta">{station.vendor} {station.model}</div>
  </div>
  <span class="status-label">{station.status === 'ONLINE' ? $t('label_online') : $t('label_offline')}</span>
</div>
