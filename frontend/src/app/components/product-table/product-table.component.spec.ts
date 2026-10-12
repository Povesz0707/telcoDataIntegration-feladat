import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { LanguageService } from '../../services/language.service';
import { IssueSeverity, Product, SourceType } from '../../models/product.models';
import { ProductTableComponent } from './product-table.component';

describe('ProductTableComponent', () => {
  let fixture: ComponentFixture<ProductTableComponent>;
  let component: ProductTableComponent;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ProductTableComponent],
      providers: [{
        provide: LanguageService,
        useValue: {
          t: (key: string) => key,
          source: (source: string) => source,
          locale: () => 'hu-HU'
        }
      }]
    }).compileComponents();

    fixture = TestBed.createComponent(ProductTableComponent);
    component = fixture.componentInstance;
  });

  afterEach(() => component.ngOnDestroy());

  it('emits a database query with pagination, sorting and combined filters', () => {
    let query: unknown;
    component.queryChanged.subscribe(value => query = value);

    component.onLazyLoad({
      first: 25,
      rows: 25,
      sortField: 'name',
      sortOrder: -1,
      globalFilter: 'cable',
      filters: {
        manufacturer: { value: 'Voltix' },
        valid: [{ value: false }]
      }
    });

    expect(query).toEqual({
      page: 1,
      size: 25,
      sort: 'name:desc',
      filter: 'cable',
      filters: {
        manufacturer: 'Voltix',
        valid: false
      }
    });
  });

  it('delays filter callbacks and clears the loading state afterwards', fakeAsync(() => {
    let filteredValue: unknown = 'not-called';

    component.scheduleFilter('name', value => filteredValue = value, 'charger');

    expect(filteredValue).toBe('not-called');
    expect(component.tableLoading()).toBeTrue();
    tick(349);
    expect(filteredValue).toBe('not-called');
    tick(1);

    expect(filteredValue).toBe('charger');
    expect(component.tableLoading()).toBeFalse();
  }));

  it('replaces a previous pending filter for the same field', fakeAsync(() => {
    let calls = 0;
    let lastValue: unknown;

    component.scheduleFilter('name', value => {
      calls++;
      lastValue = value;
    }, 'old');
    tick(100);
    component.scheduleFilter('name', value => {
      calls++;
      lastValue = value;
    }, 'new');
    tick(350);

    expect(calls).toBe(1);
    expect(lastValue).toBe('new');
  }));

  it('uses different icons and severities for errors and warnings', () => {
    const errorProduct = productWithIssue(IssueSeverity.ERROR);
    const warningProduct = productWithIssue(IssueSeverity.WARNING);

    expect(component.issueButtonIcon(errorProduct)).toBe('pi pi-times-circle');
    expect(component.issueButtonSeverity(errorProduct)).toBe('danger');
    expect(component.issueButtonIcon(warningProduct)).toBe('pi pi-exclamation-triangle');
    expect(component.issueButtonSeverity(warningProduct)).toBe('warn');
  });
});

function productWithIssue(severity: IssueSeverity): Product {
  return {
    sku: 'SKU-1',
    name: 'Test product',
    manufacturer: 'Test manufacturer',
    finalPriceHuf: 100,
    stock: 1,
    ean: null,
    updatedAt: null,
    source: 'CSV',
    primarySource: SourceType.CSV,
    valid: severity !== IssueSeverity.ERROR,
    issues: [{
      code: 'MISSING_NAME' as never,
      field: 'name',
      message: 'Test issue',
      severity
    }]
  };
}
