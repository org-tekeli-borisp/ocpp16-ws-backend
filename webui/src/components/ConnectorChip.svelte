<script lang="ts">
  import type { Connector } from '$lib/types';
  import { getConnectorClass } from '$lib/utils';

  export let connector: Connector;

  $: cls = getConnectorClass(connector.status);
</script>

<span class="connector-chip {cls}">
  <span class="cid">#{connector.connectorId}</span> {connector.status}
</span>
