<script lang="ts">
  import type { Connector } from '$lib/types';

  export let connector: Connector;

  $: cls = (() => {
    const s = connector.status.toLowerCase();
    if (s === 'available' || s === 'preparing') return 'avail';
    if (s === 'charging') return 'charg';
    if (s === 'faulted') return 'fault';
    if (s === 'unavailable') return 'unavail';
    return 'other';
  })();
</script>

<span class="connector-chip {cls}">
  <span class="cid">#{connector.connectorId}</span> {connector.status}
</span>
