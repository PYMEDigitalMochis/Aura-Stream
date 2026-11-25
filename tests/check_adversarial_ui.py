"""WebView adapter negative tests in desktop Chromium, not on Android."""
import base64
import hashlib
import json
from pathlib import Path
from playwright.sync_api import sync_playwright

ROOT=Path(__file__).resolve().parents[1]
html=(ROOT/'app/src/main/assets/viewer.html').read_text()
adapter=(ROOT/'app/src/main/assets/android_poc_ui.js').read_text()
assert hashlib.sha256(html.encode()).hexdigest()=='77d0e5d1c75303ca8dcfbbb65b0017887ae6d3a7d94d211139e9ecff289cd3fa'
r={
 'schema':'aura.audit/1','engine':'qa','source_unchanged':True,'technical_status':'incomplete',
 'editorial_status':'PENDING_HUMAN_REVIEW',
 'source':{'name':'test.mp4','sha256':'a'*64,'bytes':100,'duration_s':12,
    'streams':[{'index':0,'codec_type':'video','codec_name':'h264'}]},
 'checks':{'decode':{'status':'not_run'},'black':{'status':'not_run'},'freeze':{'status':'not_run'},
           'silence':{'status':'not_applicable'},'loudness':{'status':'not_applicable'}},
 'findings':[{'id':'F001','kind':'freeze','start_s':2,'end_s':3}],
 'not_evaluated':['decode','freeze','speech_transcription','seo','rights']
}
with sync_playwright() as p:
 browser=p.chromium.launch(executable_path='/usr/bin/chromium',headless=True,args=['--no-sandbox'])
 page=browser.new_page(viewport={'width':360,'height':800})
 errors=[];outbound=[]
 page.on('pageerror', lambda e:errors.append(str(e)))
 page.on('request',lambda e:outbound.append(e.url))
 page.set_content(html)
 encoded=base64.b64encode(json.dumps(r).encode()).decode()
 page.evaluate('x=>window.__auraAndroidReportBase64=x',encoded)
 page.add_script_tag(content=adapter)
 page.wait_for_function("document.querySelector('#results select')?.disabled === true")
 assert page.locator('#details').is_visible()
 assert page.locator('#results select').is_disabled()
 assert page.locator('#rubricItems select').first.is_disabled()
 assert page.locator('#rubricItems textarea').first.is_disabled()
 for id in ('export','exportDossier','centroImport','restore','restoreDossier','rubricCheck'):
  assert page.locator('#'+id).is_disabled(),id
 assert page.locator('body').inner_text().count('modo solo lectura') >=1
 assert not errors,errors
 assert not outbound,outbound
 print('OK: 6 unsafe controls disabled; decisions and rubric read-only after import; no page errors or external requests (Chromium only)')
 browser.close()
