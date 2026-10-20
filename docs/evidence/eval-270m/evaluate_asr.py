"""Paired official ONNX ASR packages, same raw audio/VAD/embedding/evaluator."""
from evaluate_models import *
import wave,re,unicodedata
import sherpa_onnx as s
sys.path.insert(0,str(B/'iter1'))
from text_processing import transcript_windows
from faster_whisper.vad import VadOptions,get_speech_timestamps

def chars(x):return ''.join(c for c in unicodedata.normalize('NFKC',x).lower() if c.isalnum())
def edit(a,b):
 d=list(range(len(b)+1))
 for i,c in enumerate(a,1):
  n=[i]
  for j,k in enumerate(b,1):n.append(min(n[-1]+1,d[j]+1,d[j-1]+(c!=k)))
  d=n
 return d[-1]
def main():
 import litert_lm as l
 manifest=load(B/'iter5/materials/manifest.json')['rows'];summary={}
 for date in ['2024-07-17','2025-09-09']:
  guard();dest=R/('asr-'+date);dest.mkdir(exist_ok=True);mp=B/'release/models-cache'/('sensevoice-int8-'+date)
  rec=s.OfflineRecognizer.from_sense_voice(model=str(mp/'model.int8.onnx'),tokens=str(mp/'tokens.txt'),num_threads=2,language='auto',use_itn=True)
  windows=[];proxy_edit=0;proxy_chars=0;elapsed=0
  for c in manifest:
   guard();p=dest/(c['id']+'.json')
   if not p.exists():
    with wave.open(str(B/'iter5'/c['audio'])) as f:
     assert f.getframerate()==16000 and f.getnchannels()==1 and f.getsampwidth()==2
     audio=np.frombuffer(f.readframes(f.getnframes()),dtype='<i2').astype(np.float32)/32768
    vp=R/('vad-'+c['id']+'.json')
    if not vp.exists():save(vp.name,get_speech_timestamps(audio,VadOptions(max_speech_duration_s=6,min_silence_duration_ms=300)))
    vad=load(vp);segments=[];st=time.perf_counter()
    for span in vad:
     x=rec.create_stream();x.accept_waveform(16000,audio[span['start']:span['end']]);rec.decode_stream(x);r=x.result
     if not r.text.strip():continue
     tokens=list(r.tokens);ts=list(r.timestamps);words=[]
     for i,(token,t) in enumerate(zip(tokens,ts)):
      end=ts[i+1] if i+1<len(ts) else min(t+.06,(span['end']-span['start'])/16000)
      if end>t:words.append(dict(word=token,start=span['start']/16000+t,end=span['start']/16000+end))
     segments.append(dict(start=span['start']/16000,end=span['end']/16000,text=r.text,words=words))
    save(str(p.relative_to(R)),dict(id=c['id'],segments=segments,elapsed_seconds=time.perf_counter()-st,audio_sha256=sha(B/'iter5'/c['audio']),model_sha256=sha(mp/'model.int8.onnx')))
   tr=load(p);elapsed+=tr['elapsed_seconds'];ws=transcript_windows(tr)
   for i,w in enumerate(ws):windows.append(dict(w,id=c['id']+'-onnx-'+str(i),video_id=c['id']))
   ref=chars(''.join(x['text'] for x in load(B/'iter5/transcripts_whisper'/(c['id']+'.json'))['segments']));hyp=chars(''.join(x['text'] for x in tr['segments']));proxy_edit+=edit(ref,hyp);proxy_chars+=len(ref)
   print(date,c['id'],len(ws),flush=True)
  del rec
  # Android prefixes; same fixed 740M for both ASR variants.
  cache=R/'runtime-cache';cache.mkdir(exist_ok=True)
  engine=l.EmbeddingEngine(str(B/'release/models-cache/embeddinggemma-2-740m.litertlm'),backend=l.Backend.CPU(thread_count=4),cache_dir=str(cache))
  opts=l.EmbeddingOptions(normalize=True,output_size=768)
  vp=dest/'corpus.npy'
  if vp.exists():vec=np.load(vp)
  else:
   vec=[]
   for i,w in enumerate(windows):
    vec.append(engine.compute_embedding('task: search result | text: '+w['text'],opts).embedding)
    if i%100==0:guard();print(date,'embed',i,len(windows),flush=True)
   vec=np.asarray(vec,dtype='<f4');vec/=np.linalg.norm(vec,axis=1,keepdims=True);np.save(vp,vec)
  engine.close();gold=[json.loads(x) for x in (B/'iter5/labels/gold_ai_audited.jsonl').read_text().splitlines()];qv=np.load(R/'740m-android-query.npy');th=load(R/'740m-android.json')['threshold'];p=Retriever(windows,vec,qv,gold,th);rows=evaluate(gold,windows,p);testids=set(load(B/'iter5/splits/real-test.json')['video_ids']);test=[r for r in rows if r['video_id'] in testids]
  summary[date]=dict(model_sha256=sha(mp/'model.int8.onnx'),corpus_windows=len(windows),asr_seconds=elapsed,whisper_proxy_cer=dict(edits=proxy_edit,reference_characters=proxy_chars,rate=proxy_edit/proxy_chars,notice='ASR-vs-ASR disagreement, not human CER; lowercase NFKC alphanumeric characters, full-video concatenation'),test=group(test,gold),rows=rows)
  save('asr-results.json',summary)
if __name__=='__main__':main()
