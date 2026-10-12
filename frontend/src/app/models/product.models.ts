import { IssueCode } from './issue-code.enum';

export enum SourceType {
  CSV = 'CSV',
  JSON = 'JSON'
}

export enum IssueSeverity {
  WARNING = 'WARNING',
  ERROR = 'ERROR'
}

export interface UploadResponse {
  batchId: number;
  sourceType: SourceType;
  originalFilename: string;
  rowCount: number;
  uploadedAt: string;
}

export interface ProcessingRunRequest {
  csvBatchId: number;
  jsonBatchId: number;
  primarySource: SourceType;
  fallbackMissingValues: boolean;
}

export interface ProcessingRunResponse {
  runId: number;
  primarySource: SourceType;
  fallbackMissingValues: boolean;
  status: 'RUNNING' | 'COMPLETED' | 'FAILED';
  startedAt: string;
  finishedAt: string | null;
  totalCount: number;
  validCount: number;
  invalidCount: number;
  csvFilename: string;
  jsonFilename: string;
}

export interface ProductIssue {
  code: IssueCode;
  field: string;
  message: string;
  severity: IssueSeverity;
}

export interface Product {
  sku: string | null;
  name: string | null;
  manufacturer: string | null;
  finalPriceHuf: number | null;
  stock: number | null;
  ean: string | null;
  updatedAt: string | null;
  source: string;
  primarySource: SourceType;
  valid: boolean;
  issues: ProductIssue[];
}

export interface ProductPageResponse {
  content: Product[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface ProductTableQuery {
  page: number;
  size: number;
  sort: string;
  filter?: string;
  filters: Record<string, string | boolean>;
}

export interface ProcessSelection {
  csvFile: File;
  jsonFile: File;
  primarySource: SourceType;
  fallbackMissingValues: boolean;
}

