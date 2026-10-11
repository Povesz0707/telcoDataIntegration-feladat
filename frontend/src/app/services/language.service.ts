import { HttpClient } from '@angular/common/http';
import { Injectable, signal } from '@angular/core';
import { PrimeNG } from 'primeng/config';
import { Observable, map, tap } from 'rxjs';
import { LanguageFile } from '../models/language.models';

@Injectable({ providedIn: 'root' })
export class LanguageService {
  private readonly language = signal<LanguageFile | null>(null);

  constructor(
    private readonly http: HttpClient,
    private readonly primeNG: PrimeNG
  ) {
  }

  load(): Observable<void> {
    return this.http.get<LanguageFile>('/assets/i18n/hu.json').pipe(
      tap(language => {
        this.language.set(language);
        this.primeNG.setTranslation(language.primeNg);
        document.title = language.app.documentTitle;
      }),
      map(() => undefined)
    );
  }

  t(key: string, params: Record<string, string | number> = {}): string {
    const value = key.split('.').reduce<unknown>((current, part) => {
      return current && typeof current === 'object'
        ? (current as Record<string, unknown>)[part]
        : undefined;
    }, this.language());
    if (typeof value !== 'string') {
      return key;
    }
    return value.replace(/\{(\w+)\}/g, (_, name: string) => String(params[name] ?? `{${name}}`));
  }

  source(source: string): string {
    const key = source.replace('+', '_');
    const translated = this.t(`sources.${key}`);
    return translated === `sources.${key}` ? source : translated;
  }

  issue(code: string): string {
    const translated = this.t(`issues.${code}`);
    return translated === `issues.${code}` ? code : translated;
  }

  field(field: string): string {
    const translated = this.t(`fields.${field}`);
    return translated === `fields.${field}` ? field : translated;
  }

  locale(): string {
    return 'hu-HU';
  }
}
