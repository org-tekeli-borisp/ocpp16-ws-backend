import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { get } from 'svelte/store';

describe('i18n', () => {
  beforeEach(() => {
    vi.resetModules();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  describe('translations', () => {
    it('has exactly 3 locales', async () => {
      const { translations } = await import('$lib/i18n');
      expect(Object.keys(translations)).toHaveLength(3);
      expect(translations).toHaveProperty('de');
      expect(translations).toHaveProperty('en');
      expect(translations).toHaveProperty('fr');
    });

    it('all locales have the same set of keys', async () => {
      const { translations } = await import('$lib/i18n');
      const deKeys = Object.keys(translations.de ?? {}).sort();
      const enKeys = Object.keys(translations.en ?? {}).sort();
      const frKeys = Object.keys(translations.fr ?? {}).sort();

      expect(enKeys).toEqual(deKeys);
      expect(frKeys).toEqual(deKeys);
    });

    it('has at least 120 translation keys per locale', async () => {
      const { translations } = await import('$lib/i18n');
      for (const locale of Object.keys(translations)) {
        expect(Object.keys(translations[locale] ?? {}).length).toBeGreaterThanOrEqual(120);
      }
    });
  });

  describe('t function', () => {
    it('translates key in English locale', async () => {
      vi.stubGlobal('navigator', { language: 'en-US' });
      const { locale, t } = await import('$lib/i18n');

      locale.set('en');
      const translate = get(t);

      expect(translate('page_title')).toBe('OCPP Central System');
      expect(translate('label_stations')).toBe('Stations');
    });

    it('translates key in German locale', async () => {
      vi.stubGlobal('navigator', { language: 'de-DE' });
      const { locale, t } = await import('$lib/i18n');

      locale.set('de');
      const translate = get(t);

      expect(translate('label_stations')).toBe('Stationen');
      expect(translate('label_online')).toBe('Online');
    });

    it('translates key in French locale', async () => {
      vi.stubGlobal('navigator', { language: 'fr-FR' });
      const { locale, t } = await import('$lib/i18n');

      locale.set('fr');
      const translate = get(t);

      expect(translate('label_stations')).toBe('Stations');
      expect(translate('label_online')).toBe('En ligne');
    });

    it('falls back to German when key missing in current locale', async () => {
      vi.stubGlobal('navigator', { language: 'en-US' });
      const mod = await import('$lib/i18n');

      delete (mod.translations.en as Record<string, unknown>)['__test_missing_key__'];
      (mod.translations.de ??= {})['__test_missing_key__'] = 'German fallback value';

      mod.locale.set('en');
      expect(get(mod.t)('__test_missing_key__')).toBe('German fallback value');
    });

    it('returns the key itself as last resort', async () => {
      vi.stubGlobal('navigator', { language: 'de-DE' });
      const { locale, t } = await import('$lib/i18n');

      locale.set('de');
      const translate = get(t);

      expect(translate('this.key.does.not.exist.at.all')).toBe('this.key.does.not.exist.at.all');
    });

    it('reacts to locale change', async () => {
      vi.stubGlobal('navigator', { language: 'en-US' });
      const { locale, t } = await import('$lib/i18n');

      locale.set('en');
      expect(get(t)('label_stations')).toBe('Stations');

      locale.set('de');
      expect(get(t)('label_stations')).toBe('Stationen');

      locale.set('fr');
      expect(get(t)('label_stations')).toBe('Stations');
    });

    it('locale change produces correct values across languages', async () => {
      vi.stubGlobal('navigator', { language: 'de-DE' });
      const { locale, t } = await import('$lib/i18n');

      const values: string[] = [];

      locale.set('en');
      values.push(get(t)('btn_send'));

      locale.set('de');
      values.push(get(t)('btn_send'));

      locale.set('fr');
      values.push(get(t)('btn_send'));

      expect(values).toEqual(['Send', 'Senden', 'Envoyer']);
    });
  });

  describe('detectLocale', () => {
    it('defaults to German when navigator is undefined', async () => {
      vi.stubGlobal('navigator', undefined);
      const { locale } = await import('$lib/i18n');
      expect(get(locale)).toBe('de');
    });

    it('detects English from navigator.language', async () => {
      vi.stubGlobal('navigator', { language: 'en-US' });
      const { locale } = await import('$lib/i18n');
      expect(get(locale)).toBe('en');
    });

    it('detects German from navigator.language', async () => {
      vi.stubGlobal('navigator', { language: 'de-CH' });
      const { locale } = await import('$lib/i18n');
      expect(get(locale)).toBe('de');
    });

    it('detects French from navigator.language', async () => {
      vi.stubGlobal('navigator', { language: 'fr-BE' });
      const { locale } = await import('$lib/i18n');
      expect(get(locale)).toBe('fr');
    });

    it('falls back to German for unsupported locale', async () => {
      vi.stubGlobal('navigator', { language: 'ja-JP' });
      const { locale } = await import('$lib/i18n');
      expect(get(locale)).toBe('de');
    });

    it('handles uppercase locale code', async () => {
      vi.stubGlobal('navigator', { language: 'EN-GB' });
      const { locale } = await import('$lib/i18n');
      expect(get(locale)).toBe('en');
    });

    it('handles short locale code', async () => {
      vi.stubGlobal('navigator', { language: 'fr' });
      const { locale } = await import('$lib/i18n');
      expect(get(locale)).toBe('fr');
    });
  });

  describe('locale store', () => {
    it('is writable and updates value', async () => {
      vi.stubGlobal('navigator', { language: 'de-DE' });
      const { locale } = await import('$lib/i18n');

      expect(get(locale)).toBe('de');

      locale.set('en');
      expect(get(locale)).toBe('en');

      locale.set('fr');
      expect(get(locale)).toBe('fr');
    });
  });
});
