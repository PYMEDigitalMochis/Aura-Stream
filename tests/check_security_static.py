"""Static risk gate; not a device sandbox or dynamic privacy assessment."""
from pathlib import Path
from bs4 import BeautifulSoup
import re
root=Path(__file__).resolve().parents[1]
manifest=(root/'app/src/main/AndroidManifest.xml').read_text()
gradle=(root/'app/build.gradle').read_text()
main=(root/'app/src/main/java/mx/detrasdelboton/aura/MainActivity.java').read_text()
html=(root/'app/src/main/assets/viewer.html').read_text()
assert '<uses-permission' not in manifest
assert 'usesCleartextTraffic="false"' in manifest
assert 'allowBackup="false"' in manifest
assert 'android:exported="true"' in manifest
assert 'targetSdk 35' in gradle and 'minSdk 28' in gradle
assert 'androidx.webkit:webkit:1.17.1' in gradle
for marker in ('s.setAllowFileAccess(false)','s.setAllowFileAccessFromFileURLs(false)',
               's.setAllowUniversalAccessFromFileURLs(false)',
               's.setSupportMultipleWindows(false)',
               's.setGeolocationEnabled(false)',
               's.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW)'):
 assert marker in main,marker
assert 'addJavascriptInterface' not in main
assert 'window.__auraAndroidReportBase64' in main
s=BeautifulSoup(html,'html.parser')
for tag in s.select('script[src],link[href],iframe[src],img[src],source[src]'):
 url=tag.get('src') or tag.get('href')
 assert url is None or not re.search(r'^(?:https?:|//)',url,re.I),(tag.name,url)
assert not s.find('form')
print('PASS 14 static defense assertions + external-resource and permission inventory (not Android runtime security)')
