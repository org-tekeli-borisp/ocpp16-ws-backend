<script lang="ts">
  import type { CommandField } from '$lib/types';
  import { t } from '$lib/i18n';

  export let fields: CommandField[];

  function buildPayload(): Record<string, unknown> | null {
    const payload: Record<string, unknown> = {};
    for (const f of fields) {
      const el = document.getElementById(`f_${f.name}`);
      if (!el) continue;
      let val: unknown;
      if (f.type === 'number') {
        val = el.value !== '' ? Number(el.value) : null;
      } else if (f.type === 'select') {
        val = el.value || null;
      } else if (f.type === 'json') {
        if (el.value.trim() === '') {
          if (f.required) { alert(`${$t(f.labelKey)} ${$t('field_required')}`); return null; }
          continue;
        }
        try { val = JSON.parse(el.value); }
        catch (e) { alert(`${$t('invalid_json')}: ${(e as Error).message}`); return null; }
      } else {
        val = el.value.trim() || null;
      }
      if (val === null || val === '') {
        if (f.required) { alert(`${$t(f.labelKey)} ${$t('field_required')}`); return null; }
        continue;
      }
      payload[f.name] = val;
    }
    return payload;
  }

  export function getPayload(): Record<string, unknown> | null {
    return buildPayload();
  }
</script>

{#each fields as f}
  <div class="form-group">
    <label for="f_{f.name}">
      {$t(f.labelKey)}
      {#if f.required}<span class="required">*</span>{/if}
      {#if !f.required}<span class="optional">{$t('optional')}</span>{/if}
    </label>
    {#if f.type === 'select'}
      <select id="f_{f.name}" {...(f.required ? { required: true } : {})}>
        <option value="">{$t('select_pick')}</option>
        {#each f.options || [] as opt}
          <option value={opt}>{opt}</option>
        {/each}
      </select>
    {:else if f.type === 'textarea' || f.type === 'json'}
      <textarea id="f_{f.name}" rows={f.type === 'json' ? 4 : 3}></textarea>
    {:else if f.type === 'number'}
      <input type="number" id="f_{f.name}" {...(f.required ? { required: true } : {})} />
    {:else}
      <input type="text" id="f_{f.name}" {...(f.required ? { required: true } : {})} />
    {/if}
    {#if f.hintKey}<div class="hint">{$t(f.hintKey)}</div>{/if}
  </div>
{/each}
