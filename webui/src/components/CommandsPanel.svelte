<script lang="ts">
  import type { CommandName } from '$lib/types';
  import { COMMAND_DEFINITIONS } from '$lib/commands';
  import { sendCommand } from '$lib/api/ocpp';
  import CommandSelector from '$components/CommandSelector.svelte';
  import CommandForm from '$components/CommandForm.svelte';
  import ConnectorChip from '$components/ConnectorChip.svelte';

  export let cpId: string;
  export let commands: CommandName[];
  export let connectors: Array<{connectorId: number; status: string}> = [];

  let selectedCommand: string = '';
  let response: string = '';
  let responseInfo: string = '';
  let sending = false;
  let formRef: { getPayload(): Record<string, unknown> | null } | null = null;

  function handleCommandSelect(cmd: string) {
    selectedCommand = cmd;
    response = '';
    responseInfo = '';
  }

  async function handleSend() {
    if (!selectedCommand || !cpId) return;
    sending = true;
    response = $t('waiting_response');
    responseInfo = `\u203a ${cpId} / ${selectedCommand}`;

    const payload = formRef?.getPayload() ?? {};
    if (payload === null) { sending = false; return; }

    try {
      const result = await sendCommand(cpId, selectedCommand as CommandName, payload || {});
      let formatted;
      try { formatted = JSON.stringify(JSON.parse(result.body), null, 2); }
      catch { formatted = result.body; }
      const cls = result.status >= 200 && result.status < 300 ? 'success' : 'error';
      response = `<span class="${cls}">${result.status} ${result.statusText}</span>\n\n${formatted}`;
    } catch (err) {
      response = `<span class="error">${$t('network_error')}: ${(err as Error).message}</span>`;
    } finally {
      sending = false;
    }
  }

  $: currentDef = selectedCommand ? COMMAND_DEFINITIONS[selectedCommand] : null;
</script>

<div class="panel">
  <h2>{$t('label_command')}</h2>
  <div class="panel-body">
    {#if connectors.length > 0}
      <div class="connector-list" style="margin-bottom:1rem;">
        {#each connectors as conn}
          <ConnectorChip connector={conn} />
        {/each}
      </div>
    {/if}

    <CommandSelector
      {commands}
      {selectedCommand}
      onCommandSelect={handleCommandSelect}
    />

    {#if currentDef}
      {#if currentDef.fields.length === 0}
        <p style="color:#6c757d;font-size:.85rem;">{$t('no_params')}</p>
      {:else}
        <CommandForm
          fields={currentDef.fields}
          bind:this={formRef}
        />
      {/if}
    {:else if selectedCommand}
      <p style="color:#6c757d;font-size:.85rem;">{$t('no_fields')}</p>
    {/if}

    <div style="margin-top:1rem;">
      <button
        class="btn btn-primary"
        disabled={!selectedCommand || sending}
        onclick={handleSend}
      >
        {sending ? $t('btn_sending') : $t('btn_send')}
      </button>
    </div>
  </div>
</div>

<div class="panel">
  <h2>{$t('response_label')}</h2>
  <div class="panel-body">
    <div class="response-info">{responseInfo}</div>
    <div class="response-area">
      {#if response}
        {@html response}
      {:else}
        <span class="empty">{$t('no_response_yet')}</span>
      {/if}
    </div>
  </div>
</div>
