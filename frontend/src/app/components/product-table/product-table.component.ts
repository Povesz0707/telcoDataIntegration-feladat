import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, OnDestroy, Output, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { DatePickerModule } from 'primeng/datepicker';
import { DialogModule } from 'primeng/dialog';
import { DividerModule } from 'primeng/divider';
import { InputNumberModule } from 'primeng/inputnumber';
import { InputTextModule } from 'primeng/inputtext';
import { SelectModule } from 'primeng/select';
import { SkeletonModule } from 'primeng/skeleton';
import { TableLazyLoadEvent, TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { TooltipModule } from 'primeng/tooltip';
import { NULL_FILTER_VALUE, PRODUCT_FIELDS } from '../../constants/product-fields';
import { IssueSeverity, Product, ProductIssue, ProductTableQuery, SourceType } from '../../models/product.models';
import { LanguageService } from '../../services/language.service';

@Component({
  selector: 'app-product-table',
  standalone: true,
  imports: [ButtonModule, CommonModule, DatePickerModule, DialogModule, DividerModule, FormsModule, InputNumberModule, InputTextModule, SelectModule, SkeletonModule, TableModule, TagModule, TooltipModule],
  templateUrl: './product-table.component.html'
})
export class ProductTableComponent implements OnDestroy {
  readonly productFields = PRODUCT_FIELDS;
  private readonly filterDelayMs = 350;
  private readonly filterTimers = new Map<string, ReturnType<typeof setTimeout>>();
  private readonly parsedDateFilters = new Map<string, Date>();
  readonly i18n = inject(LanguageService);
  readonly issueSeverity = IssueSeverity;
  readonly nullFilterValue = NULL_FILTER_VALUE;
  readonly sourceOptions = computed(() => [
    { label: this.i18n.source(SourceType.CSV), value: SourceType.CSV },
    { label: this.i18n.source(SourceType.JSON), value: SourceType.JSON },
    { label: this.i18n.source('CSV+JSON'), value: 'CSV+JSON' }
  ]);
  readonly statusOptions = computed(() => [
    { label: this.i18n.t('common.valid'), value: true },
    { label: this.i18n.t('common.invalid'), value: false }
  ]);
  readonly tableLoading = signal(false);
  readonly skeletonRows = Array.from({ length: 6 });
  readonly skeletonColumns = Array.from({ length: 10 });
  readonly selectedProduct = signal<Product | null>(null);
  issuesDialogVisible = false;
  @Input({ required: true }) products: Product[] = [];
  @Input() loading = false;
  @Input() totalRecords = 0;
  @Input() first = 0;
  @Input() pageSize = 10;
  @Output() readonly queryChanged = new EventEmitter<ProductTableQuery>();

  scheduleFilter(field: string, filterCallback: (value: unknown) => void, value: unknown): void {
    this.tableLoading.set(true);

    const previousTimer = this.filterTimers.get(field);
    if (previousTimer) {
      clearTimeout(previousTimer);
    }

    const timer = setTimeout(() => {
      filterCallback(value === '' ? null : value);
      this.filterTimers.delete(field);

      if (this.filterTimers.size === 0) {
        this.tableLoading.set(false);
      }
    }, this.filterDelayMs);

    this.filterTimers.set(field, timer);
  }

  filterInputValue(value: unknown): string | number | boolean | null {
    return this.isNullFilter(value) ? null : (value as string | number | boolean | null | undefined) ?? null;
  }

  dateFilterInputValue(value: unknown): Date | null {
    if (this.isNullFilter(value) || !value) {
      return null;
    }
    if (value instanceof Date) {
      return this.cachedDate(value);
    }
    if (typeof value !== 'string') {
      return null;
    }
    const [year, month, day] = value.split('-').map(Number);
    return Number.isInteger(year) && Number.isInteger(month) && Number.isInteger(day)
      ? this.cachedDate(new Date(year, month - 1, day))
      : null;
  }

  onDateFilterChange(field: string, filterCallback: (value: unknown) => void, value: Date | string | null): void {
    const date = value instanceof Date ? value : this.dateFilterInputValue(value);
    this.scheduleFilter(field, filterCallback, date ? this.formatFilterDate(date) : null);
  }

  onDateFilterClear(field: string, filterCallback: (value: unknown) => void): void {
    this.scheduleFilter(field, filterCallback, null);
  }

  private formatFilterDate(value: Date): string {
    const year = value.getFullYear();
    const month = String(value.getMonth() + 1).padStart(2, '0');
    const day = String(value.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }

  isNullFilter(value: unknown): boolean {
    return value === this.nullFilterValue;
  }

  toggleNullFilter(field: string, filterCallback: (value: unknown) => void, value: unknown): void {
    this.scheduleFilter(field, filterCallback, this.isNullFilter(value) ? null : this.nullFilterValue);
  }

  ngOnDestroy(): void {
    this.filterTimers.forEach(timer => clearTimeout(timer));
    this.filterTimers.clear();
    this.parsedDateFilters.clear();
  }

  private cachedDate(value: Date): Date {
    const key = this.formatFilterDate(value);
    const cachedValue = this.parsedDateFilters.get(key);
    if (cachedValue) {
      return cachedValue;
    }
    const stableValue = new Date(value.getFullYear(), value.getMonth(), value.getDate());
    this.parsedDateFilters.set(key, stableValue);
    return stableValue;
  }

  openIssues(product: Product): void {
    this.selectedProduct.set(product);
    this.issuesDialogVisible = true;
  }

  errorIssues(product: Product): ProductIssue[] {
    return product.issues.filter(issue => issue.severity === IssueSeverity.ERROR);
  }

  warningIssues(product: Product): ProductIssue[] {
    return product.issues.filter(issue => issue.severity === IssueSeverity.WARNING);
  }

  hasErrors(product: Product): boolean {
    return this.errorIssues(product).length > 0;
  }

  issueButtonIcon(product: Product): string {
    return this.hasErrors(product) ? 'pi pi-times-circle' : 'pi pi-exclamation-triangle';
  }

  issueButtonSeverity(product: Product): 'danger' | 'warn' {
    return this.hasErrors(product) ? 'danger' : 'warn';
  }

  onLazyLoad(event: TableLazyLoadEvent): void {
    const rows = event.rows ?? this.pageSize;
    const filters: Record<string, string | number | boolean> = {};

    Object.entries(event.filters ?? {}).forEach(([field, metadata]) => {
      const filter = Array.isArray(metadata) ? metadata[0] : metadata;
      const value = filter?.value;
      if (value !== null && value !== undefined && value !== '') {
        filters[field] = value;
      }
    });

    const sortField = event.sortField ?? this.productFields.sku;
    const sortDirection = event.sortOrder === -1 ? 'desc' : 'asc';
    this.queryChanged.emit({
      page: Math.floor((event.first ?? 0) / rows),
      size: rows,
      sort: `${sortField}:${sortDirection}`,
      filter: typeof event.globalFilter === 'string' ? event.globalFilter : undefined,
      filters
    });
  }

  formatPrice(price: number | null): string {
    return price === null
      ? this.i18n.t('common.notAvailable')
      : `${new Intl.NumberFormat(this.i18n.locale()).format(price)} ${this.i18n.t('common.currency')}`;
  }

  formatDate(value: string | null): string {
    return value === null ? this.i18n.t('common.notAvailable') : new Intl.DateTimeFormat(this.i18n.locale(), {
      dateStyle: 'short',
      timeStyle: 'short'
    }).format(new Date(value));
  }
}
