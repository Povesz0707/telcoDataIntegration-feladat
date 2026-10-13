import { ComponentFixture, TestBed } from '@angular/core/testing';
import { LanguageService } from '../../services/language.service';
import { SourceType } from '../../models/product.models';
import { UploadPanelComponent } from './upload-panel.component';

describe('UploadPanelComponent', () => {
  let fixture: ComponentFixture<UploadPanelComponent>;
  let component: UploadPanelComponent;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [UploadPanelComponent],
      providers: [{
        provide: LanguageService,
        useValue: {
          t: (key: string) => key,
          source: (source: string) => source
        }
      }]
    }).compileComponents();

    fixture = TestBed.createComponent(UploadPanelComponent);
    component = fixture.componentInstance;
  });

  it('accepts a CSV file for the CSV source', () => {
    const file = new File(['sku,name'], 'products.csv', { type: 'text/csv' });

    component.handleFileInput(fileEvent(file), SourceType.CSV);

    expect(component.csvFile).toBe(file);
    expect(component.csvError).toBeNull();
  });

  it('rejects a file with the wrong extension', () => {
    const file = new File(['{}'], 'products.csv', { type: 'application/json' });

    component.handleFileInput(fileEvent(file), SourceType.JSON);

    expect(component.jsonFile).toBeNull();
    expect(component.jsonError).toBe('upload.invalidType');
  });

  it('rejects files larger than the configured limit', () => {
    const file = new File(['data'], 'products.csv', { type: 'text/csv' });
    Object.defineProperty(file, 'size', { value: component.maxFileSize + 1 });

    component.handleFileInput(fileEvent(file), SourceType.CSV);

    expect(component.csvFile).toBeNull();
    expect(component.csvError).toBe('upload.invalidSize');
  });

  it('does not emit a processing request until both files are selected', () => {
    let emitted = false;
    component.processRequested.subscribe(() => emitted = true);

    component.startProcessing();

    expect(emitted).toBeFalse();
  });

  it('does not emit a processing request without a primary source', () => {
    const csvFile = new File(['sku,name'], 'products.csv', { type: 'text/csv' });
    const jsonFile = new File(['[]'], 'products.json', { type: 'application/json' });
    let emitted = false;
    component.processRequested.subscribe(() => emitted = true);
    component.primarySource = null;
    component.handleFileInput(fileEvent(csvFile), SourceType.CSV);
    component.handleFileInput(fileEvent(jsonFile), SourceType.JSON);

    component.startProcessing();

    expect(emitted).toBeFalse();
  });

  it('emits the selected files and processing options', () => {
    const csvFile = new File(['sku,name'], 'products.csv', { type: 'text/csv' });
    const jsonFile = new File(['[]'], 'products.json', { type: 'application/json' });
    let selection: unknown;
    component.processRequested.subscribe(value => selection = value);
    component.primarySource = SourceType.CSV;
    component.fallbackMissingValues = false;
    component.handleFileInput(fileEvent(csvFile), SourceType.CSV);
    component.handleFileInput(fileEvent(jsonFile), SourceType.JSON);

    component.startProcessing();

    expect(selection).toEqual({
      csvFile,
      jsonFile,
      primarySource: SourceType.CSV,
      fallbackMissingValues: false
    });
  });

  it('does not emit while a processing request is already busy', () => {
    const csvFile = new File(['sku,name'], 'products.csv', { type: 'text/csv' });
    const jsonFile = new File(['[]'], 'products.json', { type: 'application/json' });
    let emitted = false;
    component.processRequested.subscribe(() => emitted = true);
    component.busy = true;
    component.handleFileInput(fileEvent(csvFile), SourceType.CSV);
    component.handleFileInput(fileEvent(jsonFile), SourceType.JSON);

    component.startProcessing();

    expect(emitted).toBeFalse();
  });
});

function fileEvent(file: File): Event {
  return { target: { files: [file], value: 'selected-file' } } as unknown as Event;
}
