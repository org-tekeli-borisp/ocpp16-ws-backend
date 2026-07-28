<script lang="ts">
  import type { CommandName } from '$lib/types';
  import { t } from '$lib/i18n';
  import { COMMAND_DEFINITIONS } from '$lib/commands';
  import { sendCommand } from '$lib/api/ocpp';
  import CommandSelector from '$components/CommandSelector.svelte';
  import CommandForm from '$components/CommandForm.svelte';
  import ConnectorChip from '$components/ConnectorChip.svelte';

  export let cpId: string;
  export let commands: CommandName[];
  export let connectors: Array<{connectorId: number; status: string}> = [];
  export let onRefresh: (() => Promise<void>) | null = null;

  let refreshing = false;
  let refreshOk = false;

  async function handleRefresh() {
    if (!onRefresh) return;
    refreshing = true;
    refreshOk = false;
    try {
      await onRefresh();
      refreshOk = true;
      setTimeout(() => { refreshOk = false; }, 1500);
    } catch {
      refreshOk = false;
    } finally {
      refreshing = false;
    }
  }

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

  function buildChargingProfilePayload(p: Record<string, unknown>): Record<string, unknown> {
    const profile: Record<string, unknown> = {
      chargingProfileId: p.chargingProfileId,
      stackLevel: p.stackLevel ?? 0,
      chargingProfilePurpose: p.chargingProfilePurpose,
      chargingProfileKind: p.chargingProfileKind,
      chargingSchedule: {
        chargingRateUnit: p.chargingRateUnit,
        chargingSchedulePeriod: [
          { startPeriod: 0, limit: p.limit },
        ],
      },
    };
    if (p.duration && Number(p.duration) > 0) {
      profile.chargingSchedule.duration = Number(p.duration);
    }
    return {
      connectorId: p.connectorId,
      csChargingProfiles: profile,
    };
  }

  async function handleSend() {
    if (!selectedCommand || !cpId) return;
    sending = true;
    response = $t('waiting_response');
    responseInfo = `\u203a ${cpId} / ${selectedCommand}`;

    const payload = formRef?.getPayload() ?? {};
    if (payload === null) { sending = false; return; }

    const finalPayload = selectedCommand === 'set-charging-profile'
      ? buildChargingProfilePayload(payload)
      : payload;

    try {
      const result = await sendCommand(cpId, selectedCommand as CommandName, finalPayload);
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
  <h2>
    <span>{$t('label_command')}</span>
    {#if onRefresh}
      <div class="panel-header-right">
        <button class="btn btn-sm btn-outline" onclick={handleRefresh} disabled={refreshing}>
          {refreshing ? '...' : refreshOk ? '✓' : $t('diag_btn_refresh')}
        </button>
      </div>
    {/if}
  </h2>
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
