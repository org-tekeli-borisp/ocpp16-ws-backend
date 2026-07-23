<script lang="ts">
  import { t } from '$lib/i18n';
  import type { DiagnosticsFile } from '$lib/types';
  import { fetchDiagnostics, downloadDiagnostic, deleteDiagnostic } from '$lib/api/ocpp';
  import { formatBytes, formatDateTimeDefault as formatDateTime } from '$lib/utils';

  export let cpId: string;

  let files: DiagnosticsFile[] = [];
  let loading = false;
  let error: string | null = null;

  async function loadFiles() {
    if (!cpId) return;
    loading = true;
    error = null;
    try {
      files = await fetchDiagnostics(cpId);
    } catch (err) {
      error = (err as Error).message;
      files = [];
    } finally {
      loading = false;
    }
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

<div class="diagnostics-panel">
  <div class="panel-header">
    <h3>{$t('diag_title')}</h3>
    <button class="btn-refresh" onclick={loadFiles} disabled={loading}>
      {$t('diag_btn_refresh')}
    </button>
  </div>

  {#if error}
    <div class="error-bar">{$t('diag_error')}: {error}</div>
  {/if}

  {#if loading}
    <div class="loading-state">{$t('diag_loading')}</div>
  {:else if files.length === 0}
    <div class="empty-state">{$t('diag_no_files')}</div>
  {:else}
    <table class="diag-table">
      <thead>
        <tr>
          <th>{$t('diag_file_name')}</th>
          <th>{$t('diag_size')}</th>
          <th>{$t('diag_uploaded')}</th>
          <th class="actions-col"></th>
        </tr>
      </thead>
      <tbody>
        {#each files as file}
          <tr>
            <td class="file-name">
              <span class="icon-file">📄</span>
              <span title={file.storedName}>{file.originalName}</span>
            </td>
            <td class="size">{formatBytes(file.sizeBytes)}</td>
            <td class="date">{formatDateTime(file.uploadedAt)}</td>
            <td class="actions-col">
              <button class="btn-sm btn-download" onclick={() => handleDownload(file.storedName)}>
                {$t('diag_btn_download')}
              </button>
              <button class="btn-sm btn-delete" onclick={() => handleDelete(file.storedName)}>
                {$t('diag_btn_delete')}
              </button>
            </td>
          </tr>
        {/each}
      </tbody>
    </table>
  {/if}
</div>

<style>
  .diagnostics-panel {
    padding: 1rem 1.5rem;
    flex: 1;
    overflow-y: auto;
  }
  .panel-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 1rem;
  }
  .panel-header h3 {
    font-size: 1rem;
    font-weight: 600;
    color: #1a1a2e;
  }
  .btn-refresh {
    padding: 0.35rem 0.8rem;
    border: 1px solid #ced4da;
    border-radius: 6px;
    background: #fff;
    cursor: pointer;
    font-size: 0.82rem;
    transition: background 0.1s;
  }
  .btn-refresh:hover:not(:disabled) {
    background: #f8f9ff;
    border-color: #4a9eff;
  }
  .btn-refresh:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }
  .error-bar {
    background: #fff3f3;
    color: #c0392b;
    padding: 0.5rem 0.8rem;
    border-radius: 6px;
    font-size: 0.82rem;
    margin-bottom: 1rem;
  }
  .loading-state,
  .empty-state {
    text-align: center;
    color: #6c757d;
    padding: 3rem 1rem;
    font-size: 0.9rem;
  }
  .diag-table {
    width: 100%;
    border-collapse: collapse;
    font-size: 0.85rem;
  }
  .diag-table thead th {
    text-align: left;
    padding: 0.6rem 0.8rem;
    border-bottom: 2px solid #e8e8e8;
    color: #6c757d;
    font-weight: 600;
    font-size: 0.75rem;
    text-transform: uppercase;
    letter-spacing: 0.04em;
  }
  .diag-table tbody td {
    padding: 0.6rem 0.8rem;
    border-bottom: 1px solid #f0f0f0;
    vertical-align: middle;
  }
  .diag-table tbody tr:hover {
    background: #f8f9ff;
  }
  .file-name {
    display: flex;
    align-items: center;
    gap: 0.4rem;
    max-width: 350px;
  }
  .icon-file {
    opacity: 0.6;
    flex-shrink: 0;
  }
  .file-name span {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
  .size {
    color: #6c757d;
    font-size: 0.82rem;
    white-space: nowrap;
  }
  .date {
    color: #6c757d;
    font-size: 0.82rem;
    white-space: nowrap;
  }
  .actions-col {
    white-space: nowrap;
    width: 180px;
  }
  .btn-sm {
    padding: 0.25rem 0.6rem;
    border: 1px solid #ced4da;
    border-radius: 4px;
    background: #fff;
    cursor: pointer;
    font-size: 0.78rem;
    transition: background 0.1s;
    margin-right: 0.3rem;
  }
  .btn-download:hover {
    background: #e8f0fe;
    border-color: #4a9eff;
    color: #1a73e8;
  }
  .btn-delete:hover {
    background: #fff3f3;
    border-color: #e74c3c;
    color: #e74c3c;
  }
</style>