import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ProductApiService } from './product-api.service';
import { SourceType } from '../models/product.models';

describe('ProductApiService', () => {
  let service: ProductApiService;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        ProductApiService,
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });

    service = TestBed.inject(ProductApiService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpTesting.verify());

  it('uploads a CSV file as multipart form data', () => {
    const file = new File(['sku,name\nSKU-1,Product'], 'products.csv', { type: 'text/csv' });

    service.uploadCsv(file).subscribe();

    const request = httpTesting.expectOne('/api/uploads/csv');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual(jasmine.any(FormData));
    const uploadedFile = request.request.body.get('file') as File;
    expect(uploadedFile).toEqual(jasmine.any(File));
    expect(uploadedFile.name).toBe(file.name);
    expect(uploadedFile.size).toBe(file.size);
    request.flush({ batchId: 1, sourceType: SourceType.CSV });
  });

  it('uploads the CSV and JSON files in one multipart request', () => {
    const csvFile = new File(['sku,name\nSKU-1,Product'], 'products.csv', { type: 'text/csv' });
    const jsonFile = new File(['[]'], 'products.json', { type: 'application/json' });

    service.uploadSources(csvFile, jsonFile).subscribe();

    const request = httpTesting.expectOne('/api/uploads/sources');
    expect(request.request.method).toBe('POST');
    const formData = request.request.body as FormData;
    expect(formData.get('csvFile')).toEqual(jasmine.any(File));
    expect((formData.get('csvFile') as File).name).toBe('products.csv');
    expect(formData.get('jsonFile')).toEqual(jasmine.any(File));
    expect((formData.get('jsonFile') as File).name).toBe('products.json');
    request.flush({ csv: {}, json: {} });
  });

  it('builds the paged product query with global and field filters', () => {
    service.getProductsPage({
      runId: 7,
      page: 2,
      size: 25,
      sort: 'name:desc',
      filter: 'charger',
      filters: {
        manufacturer: 'Voltix',
        valid: false,
        updatedAt: 'null'
      }
    }).subscribe();

    const request = httpTesting.expectOne(
      '/api/products/page?runId=7&page=2&size=25&sort=name:desc&filter=charger&manufacturer=Voltix&valid=false&updatedAt=null'
    );
    expect(request.request.method).toBe('GET');
    expect(request.request.params.get('runId')).toBe('7');
    expect(request.request.params.get('page')).toBe('2');
    expect(request.request.params.get('size')).toBe('25');
    expect(request.request.params.get('sort')).toBe('name:desc');
    expect(request.request.params.get('filter')).toBe('charger');
    expect(request.request.params.get('manufacturer')).toBe('Voltix');
    expect(request.request.params.get('valid')).toBe('false');
    expect(request.request.params.get('updatedAt')).toBe('null');
    request.flush({ content: [], page: 2, size: 25, totalElements: 0, totalPages: 0 });
  });

  it('keeps the non-paginated endpoint available for a selected run', () => {
    service.getProducts({ runId: 3 }).subscribe();

    const request = httpTesting.expectOne('/api/products?runId=3');
    expect(request.request.method).toBe('GET');
    request.flush([]);
  });

  it('propagates backend errors to the caller', () => {
    let receivedStatus: number | undefined;

    service.getLatestRun().subscribe({
      error: error => receivedStatus = error.status
    });

    const request = httpTesting.expectOne('/api/processing-runs/latest');
    request.flush('backend unavailable', { status: 503, statusText: 'Service Unavailable' });

    expect(receivedStatus).toBe(503);
  });
});
