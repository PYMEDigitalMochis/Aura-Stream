/* AURA Android adapter: intentionally no network or JS bridge. */
(function () {
  'use strict';
  if (window.__auraAndroidAdapterLoaded) return;
  window.__auraAndroidAdapterLoaded = true;
  const disabled = ['reportFile', 'export', 'exportDossier', 'centroImport', 'restore', 'restoreDossier', 'rubricCheck'];
  for (const id of disabled) {
    const control = document.getElementById(id);
    if (control) {
      control.disabled = true;
      control.setAttribute('aria-disabled', 'true');
      control.title = 'El visor recibe el informe únicamente desde el selector nativo';
    }
  }
  // The embedded viewer does not have a verified decision-export path.
  // Lock review decisions as read-only to prevent silently persisting choices
  // that cannot be recovered on Android. A full editor remains available
  // separately in the verified desktop/browser build.
  const rubric = document.getElementById('rubricItems');
  if (rubric) {
    const lockRubric = () => {
      for (const control of rubric.querySelectorAll('select,textarea,input')) {
        control.disabled = true;
        control.setAttribute('aria-disabled', 'true');
        control.title = 'Rúbrica no recuperable en esta versión Android';
      }
    };
    new MutationObserver(lockRubric).observe(rubric, {childList: true, subtree: true});
    lockRubric();
  }
  const results = document.getElementById('results');
  if (results) {
    const lockDecisions = () => {
      for (const select of results.querySelectorAll('select')) {
        select.disabled = true;
        select.setAttribute('aria-disabled', 'true');
        select.title = 'Solo lectura en la APK POC; no se guardan decisiones';
      }
    };
    new MutationObserver(lockDecisions).observe(results, {childList: true, subtree: true});
    lockDecisions();
  }
  const banner = document.createElement('p');
  banner.setAttribute('role', 'status');
  banner.style.cssText = 'padding:12px;border:2px solid #eab308;margin:10px;';
  banner.textContent = 'AURA Android POC en modo solo lectura: exportaciones, restauraciones, decisiones y rúbrica sin guardado deshabilitadas en el visor. Guarda el informe original desde la pantalla nativa.';
  document.body.insertBefore(banner, document.body.firstChild);
  const encoded = window.__auraAndroidReportBase64;
  delete window.__auraAndroidReportBase64;
  if (typeof encoded !== 'string' || !encoded) return;
  try {
    if (encoded.length > 150000) throw new Error('Informe demasiado grande');
    const bytes = Uint8Array.from(atob(encoded), c => c.charCodeAt(0));
    const payload = new TextDecoder('utf-8', {fatal: true}).decode(bytes);
    if (payload.length > 100000) throw new Error('Informe demasiado grande');
    const transfer = new DataTransfer();
    transfer.items.add(new File([payload], 'AURA_informe_parcial.json', {type: 'application/json'}));
    const input = document.getElementById('reportFile');
    if (!input) throw new Error('Selector no encontrado');
    input.files = transfer.files;
    input.dispatchEvent(new Event('change', {bubbles: true}));
  } catch (_) {
    const notice = document.getElementById('notice');
    if (notice) notice.textContent = 'Importación automática no disponible. Vuelve a la pantalla nativa y guarda allí el JSON.';
  }
})();
