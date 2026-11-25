from pathlib import Path
from tempfile import TemporaryDirectory
from subprocess import run
import json
root=Path(__file__).resolve().parents[1]
package=root/'app/src/main/java/mx/detrasdelboton/aura'
with TemporaryDirectory() as temp:
    src=[package/'VideoFrameDetector.java',package/'ReportContract.java',package/'CodecAuditResult.java',package/'AudioSignalDetector.java',root/'tests/TestVideoFrames.java']
    cmd=['javac','--release','17','-Xlint:all','-d',temp,*map(str,src)]
    process=run(cmd,capture_output=True,text=True)
    assert process.returncode==0,process.stderr
    process=run(['java','-cp',temp,'mx.detrasdelboton.aura.TestVideoFrames'],capture_output=True,text=True)
    assert process.returncode==0,process.stderr
    report=json.loads(process.stdout)
    assert report['technical_status']=='incomplete'
    assert report['checks']['decode']['status']=='not_run'
    assert report['checks']['black']['status']=='completed'
    assert report['checks']['freeze']['status']=='completed'
    assert report['checks']['silence']['status']=='not_applicable'
    assert len(report['findings'])==2
    js=root/'tests/reference_v032/aura_contract.js'
    cross="const c=require(process.argv[1]);const r=JSON.parse(process.argv[2]);const z=c.validateReport(r);if(z.findings.length!==2||z.technical_status!=='incomplete')process.exit(2);console.log('PASS JSON v0.3.2 review contract');"
    check=run(['node','-e',cross,str(js),json.dumps(report)],capture_output=True,text=True)
    assert check.returncode==0,check.stderr
    print(process.stderr.strip())
    print(check.stdout.strip())
print('PASS: synthetic visual frames and Android contract; no Android APK or physical decoder evaluated')
