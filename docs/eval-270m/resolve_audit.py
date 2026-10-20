"""Read-only audit replay; no model inference, secret access, or frozen writes."""
from evaluate_models import *

def main():
 guard()
 gold={g['id']:g for g in map(json.loads,(B/'iter5/labels/gold_ai_audited.jsonl').read_text().splitlines())}
 frozen=load(B/'iter5/results/real-test.json')['rows']; old={r['id']:r for r in frozen if r['positive']}
 evidence={'method':'Frozen hit() replay on all saved rankings; exact float32 matrix comparison; calibration-only boundary replay. No new inference.'}
 compared={}; mismatches=[]
 for model in ['740m','270m','hf740m']:
  for protocol in (['historical'] if model=='hf740m' else ['historical','android']):
   tag=model+'-'+protocol; obj=load(R/(tag+'.json'))
   for split in ['dev','test']:
    for r in obj['results'][split]['rows']:
     for k,n in [('top1',1),('top3',3),('topk',10)]:
      if r[k]!=any(hit(c,gold[r['id']]) for c in r['ranking'][:n]):mismatches.append([tag,split,r['id'],k])
   rows={r['id']:r for r in obj['results']['test']['rows'] if r['positive']};assert rows.keys()==old.keys()
   compared[tag]={'hit3_n':sum(r['top3'] for r in rows.values()),'losses':sorted(k for k in old if old[k]['top3'] and not rows[k]['top3']),'gains':sorted(k for k in old if not old[k]['top3'] and rows[k]['top3'])}
 assert not mismatches
 evidence.update(frozen_hit_mismatches=mismatches,versus_frozen_HF=compared)
 matrices={}
 for kind in ['query','calibration-query','corpus','calibration']:
  a=np.load(R/('740m-android-'+kind+'.npy'));b=np.load(R/('270m-android-'+kind+'.npy'));d=np.max(np.abs(a-b),axis=1);changed=np.flatnonzero(d)
  item={'shape':list(a.shape),'identical_rows':int(sum(d==0)),'different_rows':int(len(changed)),'changed_max_abs_median':float(np.median(d[changed])) if len(changed) else 0,'max_abs':float(d.max())}
  if kind=='corpus':
   ws=load(B/'iter5/eval/corpus.json')['windows'];item['changed_rows']=[{'id':ws[int(i)]['id'],'max_abs':float(d[i])} for i in changed]
  matrices[kind]=item
 assert matrices['query']['identical_rows']==140 and matrices['calibration-query']['identical_rows']==76 and matrices['corpus']['identical_rows']==1132
 evidence['android_matrices']=matrices
 a=load(R/'740m-android.json');b=load(R/'270m-android.json');top3=[];top1=[]
 for split in ['dev','test']:
  ar={r['id']:r for r in a['results'][split]['rows'] if r['positive']};br={r['id']:r for r in b['results'][split]['rows'] if r['positive']}
  for k in ar:
   if {c['id'] for c in ar[k]['ranking'][:3]}!={c['id'] for c in br[k]['ranking'][:3]}:top3.append(k)
   if ar[k]['top1']!=br[k]['top1']:top1.append(k)
 assert not top3 and top1==['en04-q02']
 evidence['android_positive_top3_set_changes']=top3;evidence['android_positive_top1_label_changes']=top1
 calibration={}
 for model in ['740m','270m']:
  cal=load(R/(model+'-android-threshold.json'));rows=cal['rows'];scores=[r['ranking'][0]['score'] for r in rows];neg=[s for r,s in zip(rows,scores) if not r['positive']];th=threshold(scores,neg)
  assert th==cal['threshold']==0.8748952746391297
  boundary=[r for r in rows if r['id']=='neg03-zh'][0]['ranking'][0]['score'];assert th==math.nextafter(boundary,math.inf)
  calibration[model]={'threshold':th,'boundary_query':'neg03-zh','boundary_score':boundary,'negative_accepts':[r['id'] for r in rows if not r['positive'] and r['ranking'][0]['score']>=th]}
 evidence['calibration']=calibration
 for name,h in load(R/'input-manifest.json').items():
  if '/release/app/' not in name:assert sha(name)==h,name
 prior=load(B/'release/out/final-artifact-validation.json')
 for item in prior['artifacts']:assert sha(B/'release'/item['path'])==item['sha256']
 evidence['frozen_input_hashes_unchanged']=True;evidence['prior_1_0_3_artifacts_unchanged']=True
 for name in ['jingku-1.0.4-arm64-v8a.apk','jingku-1.0.4.aab']:evidence[name]={'sha256':sha(B/'release/out'/name),'bytes':(B/'release/out'/name).stat().st_size}
 evidence['decision']=load(R/'decision.json');assert evidence['decision']['decision']=='ADOPT'
 inputs=[R/(m+'-'+p+s) for m in ['740m','270m'] for p in ['historical','android'] for s in ['.json','-threshold.json','-corpus.npy','-query.npy','-calibration-query.npy']]
 inputs += [R/'hf740m-historical.json',B/'iter5/results/real-test.json',R/'270m-pinned-model-card.md']
 evidence['evidence_sha256']={str(p.relative_to(B)):sha(p) for p in inputs}
 save('audit-evidence.json',evidence)
 print(json.dumps({k:v for k,v in evidence.items() if k in ['versus_frozen_HF','calibration','android_positive_top1_label_changes','frozen_input_hashes_unchanged','prior_1_0_3_artifacts_unchanged']}))
if __name__=='__main__':main()
