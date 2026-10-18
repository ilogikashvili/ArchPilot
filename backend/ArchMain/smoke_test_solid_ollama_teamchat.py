"""SOLID transforms, Ollama keys and team chat. Run against port 8091 with Ollama (or a stand-in) on localhost:11434."""
import os,uuid,time,json,threading,requests,websocket
B=os.getenv('API_BASE','http://localhost:8091/api/v1')
def call(m,p,b=None,t=None,status=200):
 r=requests.request(m,B+p,json=b,headers={'Authorization':'Bearer '+t} if t else {},timeout=120)
 assert r.status_code==status,(m,p,r.status_code,r.text); print('PASS',m,p,r.status_code); return r.json() if r.content else None
def register(n):
 e=uuid.uuid4().hex+'@test.local';return call('POST','/auth/register',{'email':e,'password':'Password123!','displayName':n},status=201)['accessToken'],e
a,_=register('Alice');b,eb=register('Bob')
p=call('POST','/projects',{'name':'Team chat smoke'},a,201)['id'];root='/projects/'+p
N=lambda i,t,l,x,y:{'id':i,'type':t,'label':l,'description':'','x':x,'y':y,'properties':{}}
E=lambda i,s,t,ty='sync':{'id':i,'type':ty,'sourceId':s,'targetId':t,'label':'','properties':{}}
doc={'schemaVersion':'1.0','viewport':{'x':0,'y':0,'zoom':1},'metadata':{},'nodes':[N('web','web','Web',0,0),N('mob','mobile','Mobile',0,200),N('adm','web','Admin',0,400),N('orders','service','Orders Service',300,200),N('pay','service','Payments Service',600,100),
 N('db','database','Orders DB',600,300),N('stripe','external','Stripe',900,0),N('paypal','external','PayPal',900,200),N('q','queue','Kafka',900,400)],
 'edges':[E('e1','web','orders'),E('e2','mob','orders'),E('e3','adm','orders'),E('e4','orders','pay'),E('e5','orders','db','dependency'),E('e6','orders','q','event'),E('e7','pay','stripe'),E('e8','pay','paypal')]}
cur=call('GET',root+'/architecture',t=a)
call('PUT',root+'/architecture',{'document':doc,'expectedRevision':cur['revision']},a)
cat=[x['id'] for x in call('GET','/design-patterns',t=a) if x['category']=='SOLID Principles']
assert cat==['solid-srp','solid-ocp','solid-lsp','solid-isp','solid-dip','solid-all'],cat
for pid in cat:
 t=call('POST',root+'/architecture/patterns/'+pid+'/preview',{},a); assert t['applicable'],t; print('  ',pid,t['changes'][:2])
o=call('POST',root+'/architecture/patterns/solid-ocp/preview',{},a)
assert any('Strategy' in c for c in o['changes'])
r=call('POST',root+'/architecture/patterns/solid-dip/apply',t=a)
assert any('Repository' in n['label'] for n in r['architecture']['document']['nodes'])
assert not call('POST',root+'/architecture/patterns/solid-dip/preview',{},a)['applicable'],'DIP applied twice'
m=call('GET','/users/me/ai-keys/ollama/models?baseUrl=http://localhost:11434',t=a); assert m['ok'] and m['models'][0]['name']=='qwen3:4b'
assert not call('GET','/users/me/ai-keys/ollama/models?baseUrl=http://localhost:1',t=a)['ok']
k=call('POST','/users/me/ai-keys',{'provider':'OLLAMA','label':'Local','model':'qwen3:4b','baseUrl':'http://localhost:11434/api/chat'},a,201)
assert k['baseUrl']=='http://localhost:11434' and k['last4']=='local'
assert call('POST','/users/me/ai-keys/'+k['id']+'/test',t=a)['ok']
inv=call('POST',root+'/invitations',{'email':eb,'role':'EDITOR'},a); call('POST',root+'/invitations/'+inv['id']+'/accept',t=b)
# Bob listens live
events=[];ws=websocket.create_connection(B.replace('http','ws').replace('/api/v1','')+'/ws/projects/'+p+'?token='+b)
threading.Thread(target=lambda:[events.append(json.loads(ws.recv())) for _ in iter(int,1)],daemon=True).start()
call('POST',root+'/team-agents',{'name':'Architect','role':'Architect','keyId':k['id']},b,422)  # Bob can't use Alice's key
ar=call('POST',root+'/team-agents',{'name':'Architect','role':'Principal architect','keyId':k['id']},a,201)
cr=call('POST',root+'/team-agents',{'name':'Critic','role':"Devil's advocate",'keyId':k['id']},a,201)
call('POST',root+'/team-agents',{'name':'Critic!','keyId':k['id']},a,422)
call('POST',root+'/chat/messages',{'content':'@Architect what is wrong with the data layer?'},b,201)
def wait(f,s=20):
 end=time.time()+s
 while time.time()<end:
  if f():return True
  time.sleep(.3)
 return False
msgs=lambda:call('GET',root+'/chat',t=a)['messages']
assert wait(lambda:any(x['authorName']=='Critic' for x in msgs())),msgs()   # Architect mentioned @Critic -> chain
ms=msgs(); arch=[x for x in ms if x['authorName']=='Architect'][0]
assert '<think>' not in arch['content'] and arch['authorType']=='AI_AGENT'
d=call('POST',root+'/chat/discussions',{'topic':'Split the DB?','rounds':2},b,202)
call('POST',root+'/chat/discussions',{'topic':'again','rounds':1},a,422)
assert wait(lambda:any(x['content']=='Discussion finished.' for x in msgs()))
disc=[x for x in msgs() if x['discussionId']==d['discussionId'] and x['authorType']=='AI_AGENT']
assert [x['authorName'] for x in disc]==['Architect','Critic','Architect','Critic'],disc
assert any(e['type']=='chat.message' for e in events) and any(e['type']=='chat.typing' for e in events)
st=call('GET',root+'/chat',t=b); assert len(st['agents'])==2 and any(o['name']=='Bob' for o in st['online'])
call('DELETE',root+'/team-agents/'+cr['id'],t=a,status=204)
print('ALL PASSED')
