/* AURA offline review: strict, side-effect-free validation. Version 0.2.0 */
(function (root) {
'use strict';
const HASH=/^[a-f0-9]{64}$/i;
const KINDS = new Set(['black','freeze','silence','decode','loudness']);
const DECISIONS=new Set(['pending','confirmed','dismissed']);
function obj(x){ return x!==null && typeof x==='object' && !Array.isArray(x); }
function need(ok, msg){if(!ok) throw new Error(msg);}
function limited(v,max,name){need(typeof v==='string' && v.length<=max,name+' inválido');return v;}
function num(v){return typeof v==='number' && Number.isFinite(v) && v>=0;}
function validateReport(r){
 need(obj(r),'El informe no es un objeto.');
 need(r.schema==='aura.audit/1','Versión de informe incompatible.');
 need(obj(r.source),'Falta identidad del video.');
 const s=r.source;
 need(HASH.test(s.sha256||''),'Huella SHA-256 inválida.');
 need(limited(s.name,255,'Nombre').length>0,'Falta nombre de video.');
 need(Number.isSafeInteger(s.bytes)&&s.bytes>0,'Tamaño del original inválido.');
 need(num(s.duration_s)&&s.duration_s>0&&s.duration_s<=12*3600,'Duración inválida.');
 need(Array.isArray(r.findings) && r.findings.length<=2000,'Hallazgos excesivos o ausentes.');
 need(obj(r.checks),'Sin estados de ejecución.');
 need(Array.isArray(s.streams)&&s.streams.length>0&&s.streams.length<=32,'No existe inventario de pistas para determinar cobertura.');
 need(r.editorial_status==='PENDING_HUMAN_REVIEW','El motor no puede afirmar estados editoriales distintos de revisión pendiente.');
 const ids=new Set(), findings=[];
 for(const f of r.findings){
  need(obj(f),'Hallazgo ilegible.');
  const id=limited(f.id,72,'ID'); need(/^[a-z0-9_-]+$/i.test(id),'ID de hallazgo inválido.');
  need(!ids.has(id),'Hallazgos duplicados.');ids.add(id);
  const kind=limited(f.kind,32,'Tipo');need(KINDS.has(kind),'Tipo de hallazgo desconocido.');
  need(num(f.start_s)&&num(f.end_s) && f.start_s<=f.end_s && f.end_s<=s.duration_s+0.08,'Tiempos fuera de rango.');
  findings.push({id,kind,start_s:f.start_s,end_s:f.end_s});
 }
 const REQUIRED=['decode','black','freeze','silence','loudness'];
 need(Object.keys(r.checks).length===REQUIRED.length && REQUIRED.every(k=>Object.prototype.hasOwnProperty.call(r.checks,k)),'La cobertura técnica no contiene cinco controles obligatorios.');
 for (const [k,v] of Object.entries(r.checks)) {
  need(KINDS.has(k)&&obj(v),'Control desconocido.');
  need(['completed','failed','timeout','not_applicable','not_run'].includes(v.status),'Estado técnico inválido.');
 }
 need(r.technical_status==='completed' || r.technical_status==='incomplete','Estado técnico no reconocido.');
 const allComplete=REQUIRED.every(k=>['completed','not_applicable'].includes(r.checks[k].status));
 need((r.technical_status==='completed')===allComplete,'Estado técnico contradice los resultados por control.');
 const hasVideo=s.streams.some(t=>t.codec_type==='video');
 const hasAudio=s.streams.some(t=>t.codec_type==='audio');
 for(const k of REQUIRED){const applicable=k==='decode'?(hasVideo||hasAudio):['black','freeze'].includes(k)?hasVideo:hasAudio;
  need((r.checks[k].status==='not_applicable')===!applicable,'Control incompatible con las pistas del video: '+k);
 }
 if(r.checks.decode.diagnostic_count>0) need(r.checks.decode.status!=='completed','No puede ignorarse evidencia de decodificación dañada.');
 need(r.source_unchanged===true,'No está verificada la integridad del video original.');
 return {name:s.name,sha256:s.sha256.toLowerCase(),bytes:s.bytes,duration_s:s.duration_s,
   technical_status:r.technical_status,editorial_status:r.editorial_status,findings,
   checks:Object.fromEntries(Object.entries(r.checks).map(([k,v])=>[k,v.status])),
   not_evaluated:Array.isArray(r.not_evaluated)?r.not_evaluated.filter(x=>typeof x==='string').slice(0,30):[]};
}
function validateDecisions(a,report){
 need(obj(a)&&a.schema==='aura.review/1','Respaldo de decisiones incompatible.');
 need(a.source_sha256===report.sha256,'El respaldo corresponde a otro video.');
 need(obj(a.decisions),'Faltan decisiones.');
 const allowed=new Set(report.findings.map(f=>f.id));
 const d=Object.create(null);need(Object.keys(a.decisions).length<=report.findings.length,'Demasiadas decisiones.');
 for (const [k,v] of Object.entries(a.decisions)) {
  need(allowed.has(k)&&DECISIONS.has(v),'Decisión desconocida o hallazgo inexistente.');
  d[k]=v;
 }
 return d;
}
function chapterCheck(txt, duration){
 need(typeof txt==='string'&&txt.length<=10000,'Capítulos demasiado largos.');
 const lines=txt.split(/\r?\n/).filter(Boolean), times=[];
 for (const l of lines) {
   const m=l.match(/^\s*(?:(\d{1,2}):)?([0-5]?\d):([0-5]\d)\s+\S/);
   need(!!m,'Un capítulo no tiene tiempo o descripción válidos.');
   const t=(m[1]?+m[1]*3600:0)+(+m[2])*60+(+m[3]);times.push(t);
 }
 if(times.length===0) return {valid:false,reason:'No hay capítulos.'};
 if(times[0]!==0)return {valid:false,reason:'El primer capítulo debe empezar en 00:00.'};
 if(times.length<3)return {valid:false,reason:'Se esperan al menos tres capítulos.'};
 for(let i=0;i<times.length;i++) if((i&&times[i]-times[i-1]<10) || (i===times.length-1&&duration-times[i]<10))
   return {valid:false,reason:'Capítulos desordenados, repetidos o tramos menores a 10 s.'};
 if(times.some(t=>t>=duration))return {valid:false,reason:'Un capítulo excede la duración del video.'};
 return {valid:true,reason:'Estructura temporal plausible; no garantiza que YouTube la muestre.'};
}
const API={validateReport,validateDecisions,chapterCheck};
if(typeof module!=='undefined' && module.exports)module.exports=API;
root.AURACore=API;
})(typeof window!=='undefined'?window:globalThis);
