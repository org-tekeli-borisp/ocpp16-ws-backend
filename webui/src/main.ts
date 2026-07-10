import { i18nReady } from '$lib/i18n';
import { mount } from 'svelte';
import App from './App.svelte';

await i18nReady;

const target = document.getElementById('app');
if (!target) throw new Error('Element #app not found');

mount(App, { target });
