import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import {
  Product,
  ProcessingRunRequest,
  ProcessingRunResponse,
  SourceType,
  UploadResponse
} from '../models/product.models';

@Injectable({ providedIn: 'root' })
export class ProductApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api';

  uploadCsv(file: File): Observable<UploadResponse> {
    return this.upload('/uploads/csv', file);
  }

  uploadJson(file: File): Observable<UploadResponse> {
    return this.upload('/uploads/json', file);
  }

  process(request: ProcessingRunRequest): Observable<ProcessingRunResponse> {
    return this.http.post<ProcessingRunResponse>(`${this.baseUrl}/processing-runs`, request);
  }

  getLatestRun(): Observable<ProcessingRunResponse> {
    return this.http.get<ProcessingRunResponse>(`${this.baseUrl}/processing-runs/latest`);
  }

  getProcessingRuns(): Observable<ProcessingRunResponse[]> {
    return this.http.get<ProcessingRunResponse[]>(`${this.baseUrl}/processing-runs`);
  }

  getProducts(options: { runId: number }): Observable<Product[]> {
    const params = new HttpParams().set('runId', options.runId);
    return this.http.get<Product[]>(`${this.baseUrl}/products`, { params });
  }

  private upload(path: string, file: File): Observable<UploadResponse> {
    const formData = new FormData();
    formData.append('file', file, file.name);
    return this.http.post<UploadResponse>(`${this.baseUrl}${path}`, formData);
  }
}
