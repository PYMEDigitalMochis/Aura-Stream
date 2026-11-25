#!/usr/bin/env python3
"""Fail-closed release gate for AURA Android debug APK. Requires Android SDK binary tools.
The script does NOT build, sign, install, or upload a file. It validates only an
existing build, and its results never substitute real-device validation.
"""
from __future__ import annotations
import argparse
import hashlib
import json
import re
import subprocess
import sys
import zipfile
from pathlib import Path

EXPECTED_PACKAGE = 'mx.detrasdelboton.aura'
EXPECTED_VERSION = '0.1.8-qa'
EXPECTED_MINSDK = 28
EXPECTED_TARGETSDK = 35
MAX_APK = 60 * 1024 * 1024
MAX_UNPACKED_APK = 200 * 1024 * 1024
MAX_APK_ENTRIES = 4096


def require(condition, message):
    # Security decisions must NOT vanish with python -O / PYTHONOPTIMIZE.
    if not condition:
        raise ValueError(message)


def sha(data):
    return hashlib.sha256(data).hexdigest()


def run_binary(path, *args):
    p = subprocess.run([str(path), *map(str, args)], capture_output=True, text=True,
                       timeout=60, check=False)
    if p.returncode != 0:
        raise ValueError(f'{path.name} returned {p.returncode}: {p.stderr[-600:]}')
    return p.stdout + '\n' + p.stderr


def check(apk, sdk, src):
    apk = Path(apk).resolve()
    sdk = Path(sdk).resolve()
    src = Path(src).resolve()
    require(apk.is_file() and 1024 < apk.stat().st_size < MAX_APK, 'APK missing or implausible size')
    toolroot=sdk/'build-tools'/'36.0.0'
    aapt=toolroot/'aapt'
    apksigner=toolroot/'apksigner'
    require(aapt.is_file() and apksigner.is_file(), 'Official Android build-tools missing')
    with zipfile.ZipFile(apk) as z:
        # ZIP bombs must be rejected BEFORE attempting decompression/CRC.
        infos=z.infolist()
        names=[v.filename for v in infos]
        require(len(infos)<=MAX_APK_ENTRIES, 'Too many APK entries')
        require(sum(v.file_size for v in infos)<=MAX_UNPACKED_APK, 'APK expansion limit exceeded')
        require(all(not v.filename.startswith('/') and not any(seg in ('', '..') for seg in v.filename.rstrip('/').split('/')) for v in infos), 'Unsafe APK entry path')
        require(len(names)==len(set(names)), 'Duplicate APK entry')
        require('AndroidManifest.xml' in names, 'Missing Android manifest')
        require('classes.dex' in names, 'Missing DEX')
        require(z.testzip() is None, 'APK archive CRC failure')
        for asset in ('viewer.html', 'android_poc_ui.js'):
            assetkey='assets/'+asset
            require(assetkey in names, f'Missing expected asset {asset}')
            on_disk=(src/'app/src/main/assets'/asset).read_bytes()
            require(sha(z.read(assetkey))==sha(on_disk), f'APK asset was modified: {asset}')
    signature=run_binary(apksigner,'verify','--verbose','--print-certs',apk)
    # The substring 'verified' also matches 'NOT verified': fail closed.
    require(re.search(r'(?im)^Verified using v[123](?:\.1)? scheme[^\n]*:\s*true\b', signature) is not None,
            'Signature verification missing or false')
    badging=run_binary(aapt,'dump','badging',apk)
    require(f"package: name='{EXPECTED_PACKAGE}'" in badging, 'Unexpected package ID')
    require(f"versionName='{EXPECTED_VERSION}'" in badging, 'Unexpected version')
    require(f"sdkVersion:'{EXPECTED_MINSDK}'" in badging, 'Unexpected min SDK')
    require(f"targetSdkVersion:'{EXPECTED_TARGETSDK}'" in badging, 'Unexpected target SDK')
    permission_output=run_binary(aapt,'dump','permissions',apk)
    perms=re.findall(r"uses-permission(?:-sdk-23)?:\s+name='([^']+)'", permission_output)
    # AURA deliberately does not request Android permissions. Any permission
    # added by manifest merger is an actionable blocker for a pre-physical gate.
    require(not perms, 'Unexpected permissions: '+repr(perms))
    prohibited=['android.permission.INTERNET','android.permission.CAMERA',
                'android.permission.RECORD_AUDIO','android.permission.READ_EXTERNAL_STORAGE',
                'android.permission.WRITE_EXTERNAL_STORAGE','android.permission.MANAGE_EXTERNAL_STORAGE']
    require(not any(p in badging or p in permission_output for p in prohibited), 'Dangerous permission declared')
    return {
        'status':'VERIFICADO_COMPILACION_NO_ANDROID_FISICO',
        'apk':apk.name, 'apk_sha256':sha(apk.read_bytes()),
        'apk_size':apk.stat().st_size,
        'package':EXPECTED_PACKAGE,'version':EXPECTED_VERSION,
        'min_sdk':EXPECTED_MINSDK,'target_sdk':EXPECTED_TARGETSDK,
        'permissions':perms,
        'assets_sha256':{name:sha((src/'app/src/main/assets'/name).read_bytes()) for name in ('viewer.html','android_poc_ui.js')},
        'signature_summary':signature[-1100:].strip(),
        'warning':'No se ha probado ni instalado en un dispositivo Android.'
    }


if __name__=='__main__':
    p=argparse.ArgumentParser()
    p.add_argument('--apk',required=True)
    p.add_argument('--sdk',required=True)
    p.add_argument('--source',default=str(Path(__file__).resolve().parents[1]))
    p.add_argument('--output')
    a=p.parse_args()
    try:
        result=check(a.apk,a.sdk,a.source)
        report=json.dumps(result,ensure_ascii=False,indent=2)+'\n'
        if a.output:Path(a.output).write_text(report,encoding='utf8')
        print(report)
    except Exception as e:
        print('BLOQUEADO: APK no autorizada para prueba física: '+str(e),file=sys.stderr)
        sys.exit(2)
