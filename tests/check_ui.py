"""Browser-only smoke test of the Android WebView adapter. NOT an Android WebView test."""
from pathlib import Path
from playwright.sync_api import sync_playwright
root=Path(__file__).resolve().parents[1]
viewer=(root/'app/src/main/assets/viewer.html').read_text()
adapter=(root/'app/src/main/assets/android_poc_ui.js').read_text()
with sync_playwright() as p:
    browser=p.chromium.launch(executable_path='/usr/bin/chromium',headless=True,args=['--no-sandbox'])
    page=browser.new_page(viewport={'width':360,'height':800})
    requests=[]; errors=[]
    page.on('request',lambda r: requests.append(r.url))
    page.on('pageerror',lambda e: errors.append(str(e)))
    page.set_content(viewer)
    assert page.locator('#reportFile').count()==1
    page.evaluate('window.__auraAndroidReportBase64=null')
    page.add_script_tag(content=adapter)
    for el in ('#export','#exportDossier','#centroImport'):
        assert page.locator(el).is_disabled(),el
    assert 'exportaciones, restauraciones, decisiones y rúbrica sin guardado deshabilitadas' in page.locator('body').inner_text()
    assert page.locator('#details').is_hidden()
    assert len(requests)==0,requests
    page.close()
    page=browser.new_page(viewport={'width':360,'height':800})
    page.on('request',lambda r: requests.append(r.url))
    page.on('pageerror',lambda e: errors.append(str(e)))
    page.set_content(viewer)
    import subprocess,tempfile,json
    with tempfile.TemporaryDirectory() as tmp:
        j=root/'app/src/main/java/mx/detrasdelboton/aura/ReportContract.java'
        detector=root/'app/src/main/java/mx/detrasdelboton/aura/VideoFrameDetector.java'
        t=root/'tests/TestReportContract.java'
        subprocess.run(['javac','-d',tmp,str(j),str(detector),str(j.parent/'AudioSignalDetector.java'),str(j.parent/'CodecAuditResult.java'),str(t)],check=True)
        result=subprocess.run(['java','-cp',tmp,'TestReportContract'],text=True,capture_output=True,check=True)
        data=json.loads(result.stdout)
        import base64
        encoded=base64.b64encode(json.dumps(data,ensure_ascii=False).encode('utf-8')).decode('ascii')
        page.evaluate('value=>{window.__auraAndroidReportBase64=value}',encoded)
        page.add_script_tag(content=adapter)
        page.wait_for_function("document.getElementById('details')?.hidden===false",timeout=5000)
        assert page.locator('#identity').inner_text().strip()
        assert page.locator('#coverage').inner_text().strip()
        assert page.locator('#export').is_disabled()
        assert page.locator('#centroImport').is_disabled()
        assert 'not_run' in json.dumps(data)
    assert len(requests)==0,requests
    assert not errors,errors
    browser.close()
print('OK: Chromium 360px adapter injects accepted JSON, disables unverified exports, no network; NOT Android physical QA')
