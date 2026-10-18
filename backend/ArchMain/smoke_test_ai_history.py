"""Run against port 8091; AI calls require configured provider or local fake provider."""
import os,uuid,requests
B=os.getenv('API_BASE','http://localhost:8091/api/v1')
def call(m,p,b=None,t=None,status=200):
 r=requests.request(m,B+p,json=b,headers={'Authorization':'Bearer '+t} if t else {},timeout=110)
 assert r.status_code==status,(m,p,r.status_code,r.text)
 print('PASS',m,p,r.status_code)
 return r.json() if r.content else None
def register():
 e=uuid.uuid4().hex+'@test.local';return e,call('POST','/auth/register',{'email':e,'password':'Password123!','displayName':'Smoke'},status=201)['accessToken']
e,a=register();e2,b=register();p=call('POST','/projects',{'name':'AI history smoke'},a,201)['id'];root='/projects/'+p
v=call('POST',root+'/versions',{'message':'Initial commit'},a,201)
assert v['parentVersionId'] is None and v['author']['name']=='Smoke'
k=call('POST','/users/me/ai-keys',{'provider':'OPENAI','label':'Local fake','apiKey':'fake-secret-1234'},a,201)
assert k['last4']=='1234' and 'encryptedKey' not in k and 'apiKey' not in k
call('GET','/users/me/ai-keys',t=a)
assert call('POST','/users/me/ai-keys/'+k['id']+'/test',t=a)['ok']
call('POST','/users/me/ai-keys/'+k['id']+'/test',t=b,status=404)
r=call('POST',root+'/ai/agent-runs',{'keyId':k['id'],'prompt':'Add AI service'},a)
assert r['status']=='SUCCEEDED',r
assert any(n['id']=='ai-node' for n in call('GET',root+'/architecture',t=a)['document']['nodes'])
assert call('GET',root+'/ai/agent-runs',t=a)[0]['id']==r['id']
v2=call('POST',root+'/versions',{'name':'Second','message':'AI service'},a,201)
assert v2['parentVersionId']==v['id'] and v2['changeSummary']['nodes']['added']==1
assert len(call('GET',root+'/versions/'+v2['id']+'/diff',t=a)['nodes']['added'])==1
branch=call('POST',root+'/branches',{'name':'feature'},a)
bv=call('POST',root+'/versions',{'message':'Feature snapshot','branchId':branch['id']},a,201)
assert bv['branch']['id']==branch['id']
assert len(call('GET',root+'/versions?branchId='+branch['id'],t=a))==1
assert len(call('GET',root+'/versions?branchId=main',t=a))==2
bad=call('POST','/users/me/ai-keys',{'provider':'OPENAI','label':'bad','apiKey':'bad-key'},a,201)
assert not call('POST','/users/me/ai-keys/'+bad['id']+'/test',t=a)['ok']
assert call('POST',root+'/ai/agent-runs',{'keyId':bad['id'],'prompt':'Fail'},a)['status']=='FAILED'
for provider in ['ANTHROPIC','GEMINI','OPENROUTER']:
 q=call('POST','/users/me/ai-keys',{'provider':provider,'label':provider,'apiKey':'fake-1234'},a,201)
 assert call('POST','/users/me/ai-keys/'+q['id']+'/test',t=a)['ok']
i=call('POST',root+'/invitations',{'email':e2,'role':'EDITOR'},a)
assert call('GET','/users/me/invitations',t=b)[0]['id']==i['id']
call('GET',root+'/invitations',t=a)
call('POST',root+'/invitations/'+i['id']+'/accept',t=b)
members=call('GET',root+'/members',t=a);uid=next(x['userId'] for x in members if x['role']=='EDITOR')
call('PATCH',root+'/members/'+uid,{'role':'VIEWER'},a)
call('POST',root+'/ai/agent-runs',{'keyId':k['id'],'prompt':'denied'},b,status=403)
call('DELETE',root+'/members/'+uid,t=a,status=204)
i=call('POST',root+'/invitations',{'email':e2,'role':'EDITOR'},a)
assert call('POST',root+'/invitations/'+i['id']+'/decline',t=b)['status']=='DECLINED'
i=call('POST',root+'/invitations',{'email':e2,'role':'EDITOR'},a)
assert call('DELETE',root+'/invitations/'+i['id'],t=a)['status']=='CANCELLED'
call('DELETE','/users/me/ai-keys/'+k['id'],t=a,status=204)
print('ALL AI, HISTORY, INVITATION AND MEMBERSHIP CHECKS PASSED')
