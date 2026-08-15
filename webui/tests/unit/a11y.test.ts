import { describe, it, expect, afterEach, vi } from 'vitest';
import { mount, unmount, tick } from 'svelte';
import type { ChargePoint } from '$lib/types';
import StationItem from '$components/StationItem.svelte';
import CommandSelector from '$components/CommandSelector.svelte';
import MessagesPanel from '$components/MessagesPanel.svelte';
import TransactionsPanel from '$components/TransactionsPanel.svelte';

vi.mock('$lib/api/ocpp', () => ({
  fetchMessages: vi.fn().mockResolvedValue([]),
  fetchTransactions: vi.fn().mockResolvedValue([]),
}));

const station: ChargePoint = {
  chargePointId: 'CP01',
  vendor: 'Acme',
  model: 'X1',
  status: 'ONLINE',
} as ChargePoint;

let component: unknown = null;
let target: HTMLDivElement | null = null;

function mountTo(comp: unknown, props: Record<string, unknown>): HTMLDivElement {
  const el = document.createElement('div');
  document.body.appendChild(el);
  component = mount(comp as never, { target: el, props: props as never });
  target = el;
  return el;
}

afterEach(() => {
  if (component) {
    unmount(component as never);
    component = null;
  }
  target?.remove();
  target = null;
});

describe('StationItem accessibility', () => {
  it('renders a native button element without redundant role or aria-selected', () => {
    const el = mountTo(StationItem, {
      station,
      isActive: true,
      onSelect: vi.fn(),
    });
    const item = el.querySelector('.station-item');

    expect(item?.tagName).toBe('BUTTON');
    expect(item?.getAttribute('role')).toBeNull();
    expect(item?.getAttribute('aria-selected')).toBeNull();
  });

  it('exposes selection state via aria-current', () => {
    const el = mountTo(StationItem, {
      station,
      isActive: true,
      onSelect: vi.fn(),
    });
    expect(el.querySelector('.station-item')?.getAttribute('aria-current')).toBe('true');
  });

  it('invokes onSelect when clicked', () => {
    const onSelect = vi.fn();
    const el = mountTo(StationItem, {
      station,
      isActive: false,
      onSelect,
    });
    (el.querySelector('.station-item') as HTMLButtonElement).click();
    expect(onSelect).toHaveBeenCalledWith('CP01');
  });
});

describe('CommandSelector accessibility', () => {
  it('associates the command label with the select control', () => {
    const el = mountTo(CommandSelector, {
      commands: ['Heartbeat', 'Reset'],
      onCommandSelect: vi.fn(),
      selectedCommand: '',
    });
    const label = el.querySelector('.form-group label');
    const select = el.querySelector('.form-group select');

    expect(label?.getAttribute('for')).toBeTruthy();
    expect(select?.id).toBe(label?.getAttribute('for'));
  });
});

describe('MessagesPanel accessibility', () => {
  it('associates every filter label with its control', async () => {
    const el = mountTo(MessagesPanel, { cpId: 'CP01' });
    await tick();

    const labels = el.querySelectorAll('.filters label');
    expect(labels.length).toBe(2);
    labels.forEach((label) => {
      const forAttr = label.getAttribute('for');
      expect(forAttr).toBeTruthy();
      expect(el.querySelector(`#${forAttr}`)).not.toBeNull();
    });
  });
});

describe('TransactionsPanel accessibility', () => {
  it('associates the connector filter label with its control', async () => {
    const el = mountTo(TransactionsPanel, { cpId: 'CP01' });
    await tick();

    const label = el.querySelector('.filters label');
    expect(label?.getAttribute('for')).toBeTruthy();
    expect(el.querySelector(`#${label?.getAttribute('for') ?? ''}`)).not.toBeNull();
  });
});
