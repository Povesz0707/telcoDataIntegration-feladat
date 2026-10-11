import { IssueCode } from './issue-code.enum';

export interface LanguageFile {
  app: {
    brand: string;
    documentTitle: string;
    title: string;
  };
  common: {
    csv: string;
    json: string;
    notAvailable: string;
    none: string;
    yes: string;
    no: string;
    valid: string;
    invalid: string;
    selected: string;
    currency: string;
  };
  sources: Record<string, string>;
  fields: Record<string, string>;
  upload: {
    open: string;
    dialogTitle: string;
    description: string;
    csvSource: string;
    csvDescription: string;
    jsonSource: string;
    jsonDescription: string;
    dropHint: string;
    dropActive: string;
    removeFile: string;
    invalidType: string;
    invalidSize: string;
    primarySource: string;
    fallbackMissingValues: string;
    process: string;
  };
  processing: {
    loading: string;
    ariaLabel: string;
    historyTitle: string;
    historyEmpty: string;
    historyCount: string;
    historyLast: string;
    historyCurrent: string;
    historyColumns: {
      runId: string;
      startedAt: string;
      totalRecords: string;
      valid: string;
      invalid: string;
      primarySource: string;
      fallbackMissingValues: string;
      files: string;
      active: string;
    };
  };
  products: {
    title: string;
    latestRun: string;
    runSummary: string;
    emptyRun: string;
    emptyTable: string;
    search: string;
    searchPlaceholder: string;
    sort: string;
    onlyValid: string;
    refresh: string;
    filters: {
      contains: string;
      any: string;
    };
    columns: Record<string, string>;
    sortOptions: Record<string, string>;
  };
  issues: Record<IssueCode | 'dialogTitle' | 'errorGroup' | 'warningGroup' | 'empty' | 'openAriaLabel', string>;
  errors: Record<string, string>;
  primeNg: Record<string, unknown>;
}
