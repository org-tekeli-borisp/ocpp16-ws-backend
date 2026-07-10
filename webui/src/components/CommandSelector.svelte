<script lang="ts">
  import { COMMAND_DEFINITIONS } from '$lib/commands';

  export let commands: string[];
  export let onCommandSelect: (cmd: string) => void;
  export let selectedCommand: string = '';

  function getLabel(cmd: string): string {
    const def = COMMAND_DEFINITIONS[cmd];
    return def ? $t(def.labelKey) : cmd;
  }

  function handleChange(e: Event) {
    const val = (e.target as HTMLSelectElement).value;
    selectedCommand = val;
    onCommandSelect(val);
  }
</script>

<div class="form-group">
  <label>{$t('label_command')}</label>
  <select value={selectedCommand} onchange={handleChange}>
    <option value="">{$t('select_command_pick')}</option>
    {#each commands as cmd}
      <option value={cmd}>{getLabel(cmd)}</option>
    {/each}
  </select>
</div>
