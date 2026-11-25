from pathlib import Path
import subprocess,tempfile,json
root=Path(__file__).resolve().parents[1]
J=root/'app/src/main/java/mx/detrasdelboton/aura'
with tempfile.TemporaryDirectory() as td:
    files=[J/(x+'.java') for x in ('AudioSignalDetector','CodecAuditResult','VideoFrameDetector','ReportContract')]
    files+=list((root/'tests').glob('TestAudioSignalDetector.java'))+list((root/'tests').glob('TestAudioAdversarial.java'))+list((root/'tests').glob('TestCodecAuditContract.java'))
    p=subprocess.run(['javac','--release','17','-Xlint:all','-d',td,*map(str,files)],capture_output=True,text=True)
    assert p.returncode==0,p.stderr
    for cls in ('TestAudioSignalDetector','TestAudioAdversarial','TestCodecAuditContract'):
        full='mx.detrasdelboton.aura.'+cls
        r=subprocess.run(['java','-ea','-cp',td,full],capture_output=True,text=True)
        assert r.returncode==0,r.stdout+r.stderr
        if cls=='TestCodecAuditContract':
            obj=json.loads(r.stdout)
            assert obj['checks']['decode']['status']=='completed'
            assert obj['checks']['silence']['status']=='completed'
            assert obj['checks']['loudness']['status']=='completed'
            assert obj['technical_status']=='completed'
            assert len(obj['findings'])==1 and obj['findings'][0]['kind']=='silence'
            assert obj['editorial_status']=='PENDING_HUMAN_REVIEW'
            assert obj['metrics']['audio_tracks'][0]['method'].endswith('not LUFS or true peak')
            js=root/'tests/reference_v032/aura_contract.js'
            z=subprocess.run(['node','-e',"const c=require(process.argv[1]);const r=JSON.parse(process.argv[2]);c.validateReport(r);console.log('PASS contrato JS');",str(js),json.dumps(obj)],capture_output=True,text=True)
            assert z.returncode==0,z.stdout+z.stderr
            print(z.stdout.strip())
        else: print(r.stdout.strip())
print('PASS PCM puro + DTO JSON AURA, NO MediaCodec Android real')
