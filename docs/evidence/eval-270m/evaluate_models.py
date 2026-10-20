"""Read-only reuse of frozen iter1/iter5 data; writes only this directory."""
import os
os.environ.update(OPENBLAS_NUM_THREADS='1',OMP_NUM_THREADS='4',HF_HUB_OFFLINE='1',TRANSFORMERS_OFFLINE='1')
import sys,json,hashlib,time,math,resource,shutil,argparse
from pathlib import Path
import numpy as np
R=Path(__file__).resolve().parent; B=R.parents[1]
sys.path.insert(0,str(B/'iter5/eval'))
from eval_real import evaluate,summarize,bootstrap,hit

def load(p):return json.loads(Path(p).read_text())
def save(name,x): (R/name).write_text(json.dumps(x,ensure_ascii=False,indent=2,allow_nan=False)+'\n')
def sha(p):
 h=hashlib.sha256()
 with Path(p).open('rb') as f:
  for b in iter(lambda:f.read(1048576),b''):h.update(b)
 return h.hexdigest()
def guard():
 if shutil.disk_usage(R).free<8*1024**3:raise RuntimeError('STOP disk below 8 GiB')
def wilson(k,n):
 if not n:return None
 z=1.959963984540054;p=k/n;d=1+z*z/n;c=(p+z*z/(2*n))/d;w=z*math.sqrt(p*(1-p)/n+z*z/(4*n*n))/d
 return [c-w,c+w]
def threshold(scores,negative):
 cand=sorted(set(scores+[math.nextafter(x,math.inf) for x in scores]))
 return min(t for t in cand if sum(s>=t for s in negative)/len(negative)<=.1)
def flag_stats(rows,th):
 wrong=[not (r['positive'] and r['top1']) for r in rows];flag=[r['ranking'][0]['score']<th for r in rows];tp=sum(w and f for w,f in zip(wrong,flag));n=sum(flag);wn=sum(wrong)
 return dict(threshold=th,n=len(rows),wrong_top1_n=wn,flagged_n=n,true_positive_n=tp,precision=tp/n if n else None,recall=tp/wn if wn else None)
def group(rs,gold):
 out=dict(metrics=summarize(rs),ci95_cluster_bootstrap=bootstrap(rs))
 for k in ['top1','top3']:
  pos=[r for r in rs if r['positive']];out[k+'_wilson95']=wilson(sum(r[k] for r in pos),len(pos))
 byid={g['id']:g for g in gold};errors=[]
 for r in rs:
  if not r['positive']:continue
  c=r['ranking'][0];a=[a for a in byid[r['id']]['answers'] if a['video_id']==c['video_id']]
  if a:errors.append(min(abs(c['t']-(a['start']+a['end'])/2) for a in a))
 out['top1_timestamp_error_s_same_video_only']=dict(n=len(errors),median=float(np.median(errors)) if errors else None,p90=float(np.percentile(errors,90)) if errors else None)
 return out
class Retriever:
 def __init__(self,ws,vec,qvec,gs,th):
  self.ws=ws;self.th=th;self.cache={}
  for g,v in zip(gs,qvec):
   s=vec@v;order=np.argsort(-s,kind='stable')[:10];self.cache[g['query']]=[dict(ws[int(i)],t=(ws[int(i)]['start']+ws[int(i)]['end'])/2,score=float(s[i])) for i in order]
 def retrieve(self,q,ws,k):return self.cache[q]
 def judge(self,q,top):return dict(answer='yes' if top[0]['score']>=self.th else 'no',best=0 if top[0]['score']>=self.th else None)

def main():
 p=argparse.ArgumentParser();p.add_argument('--model',choices=['740m','270m','hf740m'],required=True);p.add_argument('--protocol',choices=['historical','android'],required=True);a=p.parse_args();guard()
 tag=a.model+'-'+a.protocol
 if (R/(tag+'.json')).exists():raise RuntimeError('Already complete: '+tag)
 ws=load(B/'iter5/eval/corpus.json')['windows'];gold=[json.loads(l) for l in (B/'iter5/labels/gold_ai_audited.jsonl').read_text().splitlines()]
 cal=load(B/'iter1/data/sensevoice-calibration-index.json');cq=load(B/'iter1/data/calibration-queries.json')
 qp='task: search result | query: ' if a.protocol=='historical' else 'task: search query | text: ';ip='' if a.protocol=='historical' else 'task: search result | text: '
 times=[];start=time.perf_counter()
 if a.model=='hf740m':
  import torch
  from sentence_transformers import SentenceTransformer
  torch.set_num_threads(2);torch.set_num_interop_threads(1)
  e=SentenceTransformer(str(B/'bench/models'),device='cpu',model_kwargs={'dtype':torch.bfloat16,'attn_implementation':'sdpa'},config_kwargs={'vision_config':None,'audio_config':None},local_files_only=True)
  modelpath=B/'bench/models/model.safetensors'
 else:
  import litert_lm as l
  modelpath=B/'release/models-cache'/('embeddinggemma-2-740m.litertlm' if a.model=='740m' else 'embeddinggemma-2-text-270m.litertlm')
  cpu=l.Backend.CPU(thread_count=4)
  e=l.EmbeddingEngine(str(modelpath),backend=cpu,vision_backend=cpu if a.model=='740m' else None,audio_backend=cpu if a.model=='740m' else None,cache_dir=str(R/'runtime-cache'),vision_tokens_per_image=140 if a.model=='740m' else None)
  opts=l.EmbeddingOptions(normalize=True,output_size=768,vision_tokens_per_image=140 if a.model=='740m' else None)
 loaded=time.perf_counter()-start
 def encode(texts,prefix,label):
  guard();dest=R/(tag+'-'+label+'.npy')
  if dest.exists():return np.load(dest)
  arr=[];st=time.perf_counter()
  if a.model=='hf740m':arr=e.encode(texts,prompt=prefix,batch_size=8,normalize_embeddings=True,convert_to_numpy=True,show_progress_bar=False)
  else:
   for i,text in enumerate(texts):
    t=time.perf_counter();arr.append(e.compute_embedding(prefix+text,opts).embedding);times.append(dict(kind=label,seconds=time.perf_counter()-t))
    if i%100==0:guard();print(tag,label,i,len(texts),flush=True)
  arr=np.asarray(arr,dtype='<f4');assert arr.shape==(len(texts),768) and np.isfinite(arr).all();arr/=np.linalg.norm(arr,axis=1,keepdims=True);np.save(dest,arr);print(tag,label,'seconds',time.perf_counter()-st,flush=True);return arr
 vec=encode([w['text'] for w in ws],ip,'corpus');cv=encode([w['text'] for w in cal],ip,'calibration');qcv=encode([q['text'] for q in cq],qp,'calibration-query')
 scores=qcv@cv.T;order=np.argsort(-scores,axis=1,kind='stable')[:,:3];top=[float(scores[i,o[0]]) for i,o in enumerate(order)];neg=[s for q,s in zip(cq,top) if not q['targets']];th=threshold(top,neg)
 def calhit(w,q):return any(w['file']==t['file'] and abs(w['prediction_time']-(t['start']+t['end'])/2)<=2 for t in q['targets'])
 cr=[dict(id=q['id'],positive=bool(q['targets']),top1=calhit(cal[int(o[0])],q),ranking=[dict(cal[int(j)],score=float(scores[i,j])) for j in o]) for i,(q,o) in enumerate(zip(cq,order))]
 save(tag+'-threshold.json',dict(threshold=th,method='iter1 S2 lowest boundary with negative FPR <= 0.10; math.nextafter float64; calibration only',query_prefix=qp,index_prefix=ip,negative_n=len(neg),negative_false_accept_n=sum(s>=th for s in neg),flagging=flag_stats(cr,th),rows=cr))
 # Freeze calibration before heldout encoding and ranking.
 testids=set(load(B/'iter5/splits/real-test.json')['video_ids']);qv=encode([g['query'] for g in gold],qp,'query');p=Retriever(ws,vec,qv,gold,th);rows=evaluate(gold,ws,p)
 splits={s:[r for r in rows if (r['video_id'] in testids)==(s=='test')] for s in ['dev','test']}
 results={}
 for s,rs in splits.items():
  subsets={'all':rs,'zh':[r for r in rs if r['language']=='zh'],'en':[r for r in rs if r['language']=='en'],'cross_language':[r for r in rs if r['cross_language']]}
  results[s]=dict(groups={k:group(v,gold) for k,v in subsets.items()},wrong_top1_flagging=flag_stats(rs,th),rows=rs)
 if a.model=='hf740m':runtime='sentence-transformers BF16 CPU2 batch8'
 else:runtime='LiteRT-LM 0.18.1 CPU4 single requests'
 out=dict(model=a.model,protocol=a.protocol,runtime=runtime,model_sha256=sha(modelpath),model_bytes=modelpath.stat().st_size,dimension=768,query_prefix=qp,index_prefix=ip,threshold=th,load_seconds=loaded,peak_rss_mib=resource.getrusage(resource.RUSAGE_SELF).ru_maxrss/1024,timing=times,results=results)
 save(tag+'.json',out)
 print('COMPLETE',tag,'test',results['test']['groups']['all'],flush=True)
 if a.model!='hf740m':e.close()
if __name__=='__main__':main()
