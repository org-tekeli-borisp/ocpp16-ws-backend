<script lang="ts">
  import { t } from '$lib/i18n';
  import type { DiagnosticsFile } from '$lib/types';
  import { fetchDiagnostics, downloadDiagnostic, deleteDiagnostic } from '$lib/api/ocpp';
  import { formatBytes, formatDateTimeDefault as formatDateTime } from '$lib/utils';

  export let cpId: string;

   let files: DiagnosticsFile[] = [];
   let loading = false;
   let error: string | null = null;
   let refreshing = false;
   let refreshOk = false;
   let isManualRefresh = false;

   async function loadFiles() {
    if (!cpId) return;
    loading = true;
    refreshing = true;
    refreshOk = false;
    error = null;
    try {
      files = await fetchDiagnostics(cpId);
      if (isManualRefresh) {
        refreshOk = true;
        setTimeout(() => { refreshOk = false; }, 1500);
      }
    } catch (err) {
      error = (err as Error).message;
      files = [];
    } finally {
      loading = false;
      refreshing = false;
      isManualRefresh = false;
    }
  }

  function triggerManualRefresh() {
    isManualRefresh = true;
    loadFiles();
  }

  async function handleDownload(fileName: string) {
    try {
      await downloadDiagnostic(cpId, fileName);
    } catch {
      error = 'Download failed';
    }
  }

  async function handleDelete(fileName: string) {
    if (!confirm($t('diag_delete_confirm'))) return;
    try {
      await deleteDiagnostic(cpId, fileName);
      await loadFiles();
    } catch {
      error = 'Delete failed';
    }
  }

  loadFiles();
</script>

<div class="panel">
  <h2>
    <span>{$t('diag_title')}</span>
    <button class="btn btn-sm btn-outline" style="float:right" onclick={triggerManualRefresh} disabled={loading || refreshing}>
      {refreshing ? '...' : refreshOk ? '✓' : $t('diag_btn_refresh')}
    </button>
  </h2>
  <div class="panel-body">
    {#if error}
      <div class="error-state" style="margin-bottom:1rem;">{$t('diag_error')}: {error}</div>
    {/if}

    {#if loading}
      <p class="loading">{$t('diag_loading')}</p>
    {:else if files.length === 0}
      <p class="empty-state">{$t('diag_no_files')}</p>
    {:else}
      <div class="tx-table-wrap">
        <table class="tx-table">
          <thead>
            <tr>
              <th>{$t('diag_file_name')}</th>
              <th>{$t('diag_size')}</th>
              <th>{$t('diag_uploaded')}</th>
              <th class="diag-actions"></th>
            </tr>
          </thead>
          <tbody>
            {#each files as file}
              <tr>
                <td class="diag-filename">
                  <span title={file.storedName}>{file.originalName}</span>
                </td>
                <td style="white-space:nowrap; color:#6c757d;">{formatBytes(file.sizeBytes)}</td>
                <td style="white-space:nowrap; color:#6c757d;">{formatDateTime(file.uploadedAt)}</td>
                <td class="diag-actions">
                  <button class="btn btn-sm btn-outline" onclick={() => handleDownload(file.storedName)}>
                    {$t('diag_btn_download')}
                  </button>
                  <button class="btn btn-sm btn-outline btn-danger" onclick={() => handleDelete(file.storedName)}>
                    {$t('diag_btn_delete')}
                  </button>
                </td>
              </tr>
            {/each}
          </tbody>
        </table>
      </div>
    {/if}
  </div>
</div>