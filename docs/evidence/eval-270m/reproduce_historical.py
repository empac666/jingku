"""Bridge saved HF corpus and frozen scoring to a fresh query encoding."""
from evaluate_models import *
a=load(R/'hf740m-historical.json');gold=[json.loads(x) for x in (B/'iter5/labels/gold_ai_audited.jsonl').read_text().splitlines()];windows=load(B/'iter5/eval/corpus.json')['windows'];vectors=np.load(B/'iter5/results/corpus-embedding.npy');qvectors=np.load(R/'hf740m-historical-query.npy');p=Retriever(windows,vectors,qvectors,gold,load(B/'iter5/eval/threshold.json')['threshold']);rows=evaluate(gold,windows,p);old=load(B/'iter5/results/real-test.json');ids={r['id'] for r in old['rows']};rs=[r for r in rows if r['id'] in ids];changed=[]
for x,y in zip(sorted(rs,key=lambda r:r['id']),sorted(old['rows'],key=lambda r:r['id'])):
 if (x['top1'],x['top3'])!=(y['top1'],y['top3']):changed.append(dict(id=x['id'],fresh=[x['top1'],x['top3']],frozen=[y['top1'],y['top3']]))
# Re-score archived rankings with the frozen evaluator: no new inference or selection.
class Archived:
 def retrieve(self,q,ws,k):return next(r['ranking'] for r in old['rows'] if next(g['query'] for g in gold if g['id']==r['id'])==q)
 def judge(self,q,top):return dict(answer='yes' if top[0]['score']>=old['threshold'] else 'no',best=0 if top[0]['score']>=old['threshold'] else None)
replay=evaluate([g for g in gold if g['id'] in ids],windows,Archived());m=summarize(replay);assert m==old['groups']['all']['metrics']
save('historical-reproduction.json',dict(archived_ranking_replay_exact=True,archived_metrics=m,fresh_query_saved_corpus_metrics=summarize(rs),changed_rows=changed,fresh_vector_max_abs_difference=float(np.max(np.abs(np.load(R/'hf740m-historical-corpus.npy')-vectors)))),)
print('Historical replay exact; fresh query differences',changed)
