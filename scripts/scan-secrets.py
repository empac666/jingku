#!/usr/bin/env python3
"""Supplementary path/credential guard; prints counts and paths, never matching values."""
from pathlib import Path
import re,json,subprocess
root=Path(__file__).resolve().parents[1]
patterns=[re.compile(x) for x in [
 rb'-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----',
 rb'gh[pousr]_[A-Za-z0-9]{30,}',
 rb'github_pat_[A-Za-z0-9_]{30,}',
 rb'AKIA[0-9A-Z]{16}',
 rb'sk-(?:proj-)?[A-Za-z0-9_-]{32,}',
 rb'(?i)(?:api[_-]?key|access[_-]?token|secret[_-]?key)\s*[:=]\s*[\x22\x27][A-Za-z0-9+/=_-]{20,}[\x22\x27]'
]]
files=subprocess.check_output(['git','ls-files','--cached','--others','--exclude-standard','-z'],cwd=root).split(b'\0')
findings=[]
for n in files:
 if not n:continue
 p=root/n.decode()
 if not p.is_file():continue
 if p.suffix.lower() in {'.jks','.keystore','.p12','.onnx','.litertlm','.gguf','.safetensors','.pt'} or 'secrets' in p.relative_to(root).parts:
  findings.append({'path':n.decode(),'reason':'forbidden artifact'})
 if p.stat().st_size>5_000_000:continue
 data=p.read_bytes()
 if any(r.search(data) for r in patterns):findings.append({'path':n.decode(),'reason':'credential pattern'})
print(json.dumps({'checked_files':len([n for n in files if n]),'findings':len(findings),'details':findings},indent=2))
raise SystemExit(bool(findings))
