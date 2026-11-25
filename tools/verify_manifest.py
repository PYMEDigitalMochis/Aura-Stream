from pathlib import Path
import hashlib,json,sys
root=Path(sys.argv[1] if len(sys.argv)>1 else Path(__file__).resolve().parents[1]).resolve()
m=json.loads((root/'MANIFIESTO_SHA256.json').read_text())
expected={x['path']:x for x in m['files']}
actual={str(x.relative_to(root)).replace('\\','/'):x for x in root.rglob('*') if x.is_file() and x.name!='MANIFIESTO_SHA256.json' and not any(y in ('__pycache__','.git') for y in x.parts)}
if expected.keys()!=actual.keys():
    raise SystemExit('FAIL files differ missing='+str(set(expected)-set(actual))+' extra='+str(set(actual)-set(expected)))
for name,path in actual.items():
    o=expected[name]
    if path.stat().st_size!=o['bytes'] or hashlib.sha256(path.read_bytes()).hexdigest()!=o['sha256']:
        raise SystemExit('FAIL '+name)
print('PASS manifest SHA-256+bytes '+str(len(expected))+'/'+str(len(expected)))
