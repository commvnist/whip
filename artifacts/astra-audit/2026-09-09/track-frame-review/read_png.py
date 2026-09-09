import struct,zlib
from pathlib import Path

def read_png(p):
 b=Path(p).read_bytes(); assert b[:8]==b'\x89PNG\r\n\x1a\n';pos=8;data=b''
 while pos<len(b):
  n=struct.unpack('>I',b[pos:pos+4])[0];typ=b[pos+4:pos+8];d=b[pos+8:pos+8+n];pos+=12+n
  if typ==b'IHDR':w,h,depth,color,comp,fil,inter=struct.unpack('>IIBBBBB',d)
  if typ==b'IDAT':data+=d
 assert depth==8 and color in [2,6] and inter==0,(depth,color,inter)
 stride=3 if color==2 else 4;raw=zlib.decompress(data);rowlen=w*stride;out=[];prev=[0]*rowlen;pos=0
 def paeth(a,b,c):
  v=a+b-c;ds=[abs(v-a),abs(v-b),abs(v-c)];return [a,b,c][ds.index(min(ds))]
 for y in range(h):
  t=raw[pos];row=list(raw[pos+1:pos+1+rowlen]);pos+=rowlen+1
  for x in range(rowlen):
   a=row[x-stride] if x>=stride else 0;b=prev[x];c=prev[x-stride] if x>=stride else 0
   if t:row[x]=(row[x]+[0,a,b,(a+b)//2,paeth(a,b,c)][t])%256
  out.append([tuple(row[x:x+3]) for x in range(0,rowlen,stride)]);prev=row
 return w,h,out
if __name__=='__main__':
 import sys,xml.etree.ElementTree as E,re
 for f in sys.argv[1:]:
  p=Path(f);w,h,im=read_png(p);print(p)
  nodes=list(E.parse(p.with_suffix('.xml')).iter('node'))
  for n in nodes:
   a=n.attrib
   if 'Close Track Entry details'==a.get('content-desc'):
    l,t,r,b=map(int,re.findall(r'\d+',a['bounds']));dark=[(x,y) for y in range(t,b) for x in range(l,r) if max(im[y][x])<140];print('close',a['bounds'],'darkpixels',len(dark),'darkbounds', (min(x for x,y in dark),min(y for x,y in dark),max(x for x,y in dark),max(y for x,y in dark)) if dark else None)
