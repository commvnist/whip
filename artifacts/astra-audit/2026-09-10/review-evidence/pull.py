from pathlib import Path
import subprocess,re,json,hashlib,sys
serial,target=sys.argv[1:];out=Path(target);out.mkdir(parents=True,exist_ok=True);rows=[];seen=set();root='/sdcard/Download/whip-ui-catalog/'
for name in subprocess.check_output(['adb','-s',serial,'shell','ls','-1t',root],text=True).splitlines():
 canonical=re.sub(r' \(\d+\)(?=\.(png|xml)$)','',name)
 if not canonical.startswith('shared.review.') or not canonical.endswith(('.png','.xml')) or canonical in seen:continue
 seen.add(canonical);p=out/canonical
 subprocess.run(['adb','-s',serial,'pull',root+name,str(p)],check=True,stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL)
 rows.append(dict(file=canonical,original=name,sha256=hashlib.sha256(p.read_bytes()).hexdigest()))
(out/'manifest.json').write_text(json.dumps(rows,indent=2)+'\n')
print('Pulled',len(rows),'files')
