from pathlib import Path
from tempfile import TemporaryDirectory
from subprocess import run
from hashlib import sha256
import json, re, shutil, sys
root=Path(__file__).resolve().parents[1]
orig=Path(__file__).resolve().parents[1]/'app/src/main/assets/viewer.html'
viewer=root/'app/src/main/assets/viewer.html'
assert sha256(viewer.read_bytes()).hexdigest()=='77d0e5d1c75303ca8dcfbbb65b0017887ae6d3a7d94d211139e9ecff289cd3fa', 'HTML differs from verified v0.3.2!'
print('OK: viewer source matches v0.3.2, sha256='+sha256(viewer.read_bytes()).hexdigest())
manifest=(root/'app/src/main/AndroidManifest.xml').read_text()
assert '<uses-permission' not in manifest, 'Extra Android permissions'
assert 'usesCleartextTraffic="false"' in manifest
assert 'allowBackup="false"' in manifest
print('OK: manifest has no file or network permissions')
js=root/'tests/reference_v032/aura_contract.js'
assert sha256(js.read_bytes()).hexdigest()=='f2aa1086d69e84c1a74ccf2b1bbbb6a311479b5722d8eb9ea8be2225f5766828', 'AURA contract checksum mismatch'
with TemporaryDirectory() as tmp:
    d=Path(tmp)
    java=root/'app/src/main/java/mx/detrasdelboton/aura/ReportContract.java'
    detector=root/'app/src/main/java/mx/detrasdelboton/aura/VideoFrameDetector.java'
    t=root/'tests/TestReportContract.java'
    p=run(['javac','-Xlint:all','-d',str(d),str(java),str(detector),str(java.parent/'AudioSignalDetector.java'),str(java.parent/'CodecAuditResult.java'),str(t)],capture_output=True,text=True)
    assert p.returncode==0,p.stderr
    p=run(['java','-cp',str(d),'TestReportContract'],capture_output=True,text=True)
    assert p.returncode==0,p.stderr
    assert '15 entradas adversariales' in p.stderr,p.stderr
    report=json.loads(p.stdout)
    assert report['technical_status']=='incomplete'
    assert all(c['status']=='not_run' for c in report['checks'].values())
    assert report['findings']==[]
    assert len(report['source']['streams'])==2
    assert report['source']['name']=='Video "prueba" \\ñ.mp4'
    cross='''const c=require(process.argv[1]);const r=JSON.parse(process.argv[2]);const result=c.validateReport(r);if(result.technical_status!=="incomplete"||result.findings.length!==0)process.exit(8);console.log("OK: JS AURA v0.3.2 accepted Android partial JSON");'''
    p=run(['node','-e',cross,str(js),json.dumps(report)],capture_output=True,text=True)
    assert p.returncode==0, p.stderr
    print('OK: javac portable core compile + 15 adversarial rejects')
    print(p.stdout.strip())
    print('OK: JSON schema, metadata, 5 unexecuted detectors, review pending')
print('RESULT: portable checks pass; Android classes/APK/device NOT compiled or tested')
