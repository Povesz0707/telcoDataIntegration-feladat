import { CommonModule } from '@angular/common';
import { Component, Input, OnDestroy, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { FilterService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { DividerModule } from 'primeng/divider';
import { InputTextModule } from 'primeng/inputtext';
import { SelectModule } from 'primeng/select';
import { SkeletonModule } from 'primeng/skeleton';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { PRODUCT_FIELDS } from '../../constants/product-fields';
import { IssueSeverity, Product, ProductIssue, SourceType } from '../../models/product.models';
import { LanguageService } from '../../services/language.service';

@Component({
  selector: 'app-product-table',
  standalone: true,
  imports: [ButtonModule, CommonModule, DialogModule, DividerModule, FormsModule, InputTextModule, SelectModule, SkeletonModule, TableModule, TagModule],
  templateUrl: './product-table.component.html'
})
export class ProductTableComponent implements OnDestroy {
  readonly productFields = PRODUCT_FIELDS;
  private readonly filterDelayMs = 350;
  private readonly filterTimers = new Map<string, ReturnType<typeof setTimeout>>();
  private readonly filterService = inject(FilterService);
  readonly i18n = inject(LanguageService);
  readonly issueSeverity = IssueSeverity;
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

  constructor() {
    this.filterService.register('displayContains', (value: unknown, filter: unknown) => this.matchesDisplayValue(value, filter));
    this.filterService.register('displayEquals', (value: unknown, filter: unknown) => this.matchesDisplayValue(value, filter, true));
  }

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

  private matchesDisplayValue(value: unknown, filter: unknown, exact = false): boolean {
    if (filter === null || filter === undefined || String(filter).trim() === '') {
      return true;
    }

    const normalizedFilter = String(filter).trim().toLocaleLowerCase(this.i18n.locale());
    const normalizedNotAvailable = this.i18n.t('common.notAvailable').trim().toLocaleLowerCase(this.i18n.locale());
    const isNotAvailable = value === null || value === undefined || value === '';

    if (isNotAvailable) {
      return normalizedNotAvailable.includes(normalizedFilter);
    }

    const normalizedValue = String(value).trim().toLocaleLowerCase(this.i18n.locale());
    return exact ? normalizedValue === normalizedFilter : normalizedValue.includes(normalizedFilter);
  }

  ngOnDestroy(): void {
    this.filterTimers.forEach(timer => clearTimeout(timer));
    this.filterTimers.clear();
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
    if (product.issues.length === 0) {
      return 'pi pi-check';
    }

    return this.hasErrors(product) ? 'pi pi-times-circle' : 'pi pi-exclamation-triangle';
  }

  issueButtonSeverity(product: Product): 'danger' | 'warn' | 'secondary' {
    return product.issues.length === 0 ? 'secondary' : (this.hasErrors(product) ? 'danger' : 'warn');
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
