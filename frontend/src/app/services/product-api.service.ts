import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import {
  Product,
  ProductPageResponse,
  ProductTableQuery,
  ProcessingRunRequest,
  ProcessingRunResponse,
  SourceType,
  UploadResponse,
  UploadSourcesResponse
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

  uploadSources(csvFile: File, jsonFile: File): Observable<UploadSourcesResponse> {
    const formData = new FormData();
    formData.append('csvFile', csvFile, csvFile.name);
    formData.append('jsonFile', jsonFile, jsonFile.name);
    return this.http.post<UploadSourcesResponse>(`${this.baseUrl}/uploads/sources`, formData);
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

  getProductsPage(options: ProductTableQuery & { runId: number }): Observable<ProductPageResponse> {
    let params = new HttpParams()
      .set('runId', options.runId)
      .set('page', options.page)
      .set('size', options.size)
      .set('sort', options.sort);

    if (options.filter) {
      params = params.set('filter', options.filter);
    }

    Object.entries(options.filters).forEach(([field, value]) => {
      params = params.set(field, value);
    });

    return this.http.get<ProductPageResponse>(`${this.baseUrl}/products/page`, { params });
  }

  private upload(path: string, file: File): Observable<UploadResponse> {
    const formData = new FormData();
    formData.append('file', file, file.name);
    return this.http.post<UploadResponse>(`${this.baseUrl}${path}`, formData);
  }
}
