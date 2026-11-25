"""Offline negative tests of release gate logic using synthetic APK + mocked tools.
Do not confuse passing this test with cryptographic verification of a real APK.
"""
from pathlib import Path
from tempfile import TemporaryDirectory
from subprocess import run
import os, shutil, sys, zipfile
from importlib.machinery import SourceFileLoader
ROOT=Path(__file__).resolve().parents[1]
GATE=ROOT/'tools/check_apk_gate.py'
with TemporaryDirectory() as d:
 d=Path(d)
 sdk=d/'sdk/build-tools/36.0.0';sdk.mkdir(parents=True)
 aapt=sdk/'aapt'; signer=sdk/'apksigner'
 aapt.write_text('''#!/bin/sh
case "$2" in
badging) echo "package: name='mx.detrasdelboton.aura' versionCode='9' versionName='0.1.8-qa'";echo "sdkVersion:'28'";echo "targetSdkVersion:'35'";;
permissions) if [ -e "$0.has_permission" ]; then echo "uses-permission: name='android.permission.INTERNET'";fi;;
esac
''')
 signer.write_text('#!/bin/sh\necho "Verified using v3 scheme (APK Signature Scheme v3): true"\n')
 aapt.chmod(0o755);signer.chmod(0o755)
 apk=d/'dummy.apk'
 def make(mutate=False,missing=False,unsafe=False):
  with zipfile.ZipFile(apk,'w') as z:
   z.writestr('AndroidManifest.xml',b'\x00'*1200)
   z.writestr('classes.dex',b'1234')
   if unsafe:z.writestr('../escape.txt',b'x')
   for n in ('viewer.html','android_poc_ui.js'):
    if missing and n=='android_poc_ui.js':continue
    z.writestr('assets/'+n,(b'CORRUPTED' if mutate and n=='viewer.html' else (ROOT/'app/src/main/assets'/n).read_bytes()))
 def call(optimize=False):
  return run([sys.executable,*(['-O'] if optimize else []),str(GATE),'--apk',str(apk),'--sdk',str(d/'sdk'),'--source',str(ROOT)],text=True,capture_output=True,timeout=20)
 make();p=call();assert p.returncode==0,p.stderr
 (sdk/'aapt.has_permission').touch();p=call();assert p.returncode!=0 and 'permissions' in p.stderr,p.stderr
 p=call(optimize=True);assert p.returncode!=0 and 'permissions' in p.stderr,'-O bypassó control de permisos'
 (sdk/'aapt.has_permission').unlink()
 make(mutate=True);p=call();assert p.returncode!=0 and 'modified' in p.stderr,p.stderr
 p=call(optimize=True);assert p.returncode!=0 and 'modified' in p.stderr,'-O bypassó integridad de assets'
 make(missing=True);p=call();assert p.returncode!=0 and 'Missing expected asset' in p.stderr,p.stderr
 make(unsafe=True);p=call(optimize=True);assert p.returncode!=0 and 'Unsafe APK entry' in p.stderr,'-O bypassó ruta ZIP insegura'
 # A highly-compressible archive >200 MiB must be rejected before CRC/decompression.
 with zipfile.ZipFile(apk,'w',compression=zipfile.ZIP_DEFLATED) as z:
  with z.open('classes.dex','w') as big:
   block=b'\0'*1024*1024
   for _ in range(201):big.write(block)
 p=call(optimize=True);assert p.returncode!=0 and 'expansion limit' in p.stderr, 'ZIP bomb escaped early preflight'

 make();signer.write_text('#!/bin/sh\necho "NOT verified"\n');p=call();assert p.returncode!=0 and 'Signature verification' in p.stderr, 'False-positive signing claim accepted'
 make();signer.write_text('#!/bin/sh\necho "Verified using v3 scheme (APK Signature Scheme v3): false"\n');p=call();assert p.returncode!=0 and 'Signature verification' in p.stderr, 'False signature flag accepted'
 make();signer.write_text('#!/bin/sh\nexit 3\n');p=call();assert p.returncode!=0 and 'apksigner' in p.stderr,p.stderr
 print('PASS 8/8 simulated gate scenarios + 3 python -O anti-bypass tests: accepted stub, permission injection, asset tampering, missing asset, false signing claims, ZIP expansion, signature failure. NO real APK was signed.')
