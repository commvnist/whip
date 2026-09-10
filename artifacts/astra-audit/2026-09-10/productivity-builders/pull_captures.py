import pathlib,subprocess,re,json,hashlib,sys
serial,target=sys.argv[1:];out=pathlib.Path(target);out.mkdir(parents=True,exist_ok=True)
root='/sdcard/Download/whip-ui-catalog'
states={f'{base}.{state}' for base in ['tasks.today','habits.today','goals.active','tracks.all'] for state in ['populated','expanded']} | {'shared.home.populated','habits.archived.populated','habits.insights.populated','goals.insights.populated','habits.productivity-builder.timer-expanded','habits.productivity-builder.timer-stopped'}
seen=set();records=[]
for name in subprocess.check_output(['adb','-s',serial,'shell','ls','-1t',root],text=True).splitlines():
 canonical=re.sub(r' \(\d+\)(?=\.(png|xml)$)','',name)
 if canonical.rsplit('.',1)[0] not in states or not canonical.endswith(('.png','.xml')) or canonical in seen:continue
 seen.add(canonical);p=out/canonical
 subprocess.run(['adb','-s',serial,'pull',root+'/'+name,str(p)],check=True,stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL)
 records.append(dict(file=canonical,original=name,sha256=hashlib.sha256(p.read_bytes()).hexdigest()))
(out/'capture-manifest.json').write_text(json.dumps(records,indent=2)+'\n')
print('Pulled',len(records),'files')
