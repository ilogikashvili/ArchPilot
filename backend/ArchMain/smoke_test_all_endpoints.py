import requests, uuid, json, os
B=os.getenv("API_BASE","http://localhost:8091/api/v1"); S=requests.Session()
def call(m,p,body=None,tok=None,base=B,expect_ok=True):
    h={"Authorization":"Bearer "+tok} if tok else {}
    r=S.request(m,base+p,json=body,headers=h)
    bad = r.status_code>=500 or r.status_code in (404,405) and expect_ok
    print(("FAIL " if bad else "ok   ")+f"{r.status_code} {m} {p}" + (" "+r.text[:200] if bad else ""))
    try: return r.json()
    except: return None
e=f"u{uuid.uuid4().hex[:6]}@t.co"; e2=f"v{uuid.uuid4().hex[:6]}@t.co"
t=call("POST","/auth/register",{"email":e,"password":"Password123!","displayName":"U"})
t2=call("POST","/auth/register",{"email":e2,"password":"Password123!","displayName":"V"})["accessToken"]
t=call("POST","/auth/login",{"email":e,"password":"Password123!"}); A=t["accessToken"]
call("GET","/auth/me",tok=A); call("POST","/auth/refresh",{"refreshToken":t["refreshToken"]})
call("POST","/auth/forgot-password",{"email":e}); call("POST","/auth/resend-verification",{"email":e})
call("POST","/auth/verify-email",{"token":"x"},expect_ok=False); call("POST","/auth/reset-password",{"token":"x","newPassword":"Password123!"},expect_ok=False)
call("PATCH","/users/me",{"displayName":"U2"},tok=A)
p=call("POST","/projects",{"name":"P","description":"d"},tok=A); P=p["id"]
for m,x in [("GET",""),("GET",f"/{P}"),("GET",f"/{P}/summary"),("POST",f"/{P}/archive"),("POST",f"/{P}/restore")]: call(m,"/projects"+x,tok=A)
call("PATCH",f"/projects/{P}",{"name":"P2"},tok=A)
d=call("POST",f"/projects/{P}/duplicate",{"name":"Dup"},tok=A)
arch=call("GET",f"/projects/{P}/architecture",tok=A); print(json.dumps(arch)[:300])
doc={"schemaVersion":"1.0","nodes":[{"id":"n1","type":"service","label":"API","description":"","x":0,"y":0,"properties":{}},{"id":"n2","type":"database","label":"DB","description":"","x":100,"y":0,"properties":{}}],"edges":[{"id":"e1","type":"sync","sourceId":"n1","targetId":"n2","label":"q","properties":{}}],"viewport":{"x":0,"y":0,"zoom":1},"metadata":{}}
call("PUT",f"/projects/{P}/architecture",{"document":doc,"expectedRevision":arch.get("revision",0)},tok=A)
pp=f"/projects/{P}/architecture"
call("POST",pp+"/validate",doc,tok=A); call("GET",pp+"/metadata",tok=A)
call("GET",pp+"/export?format=json",tok=A); call("POST",pp+"/import/validate",doc,tok=A)
call("GET","/architecture/component-types",tok=A); call("GET","/architecture/edge-types",tok=A)
tl=call("GET","/templates",tok=A); tid=tl[0]["id"] if isinstance(tl,list) else tl["content"][0]["id"]
call("GET",f"/templates/{tid}",tok=A)
ct=call("POST","/templates",{"name":"T","description":"x","document":doc},tok=A)
call("PATCH",f"/templates/{ct['id']}",{"name":"T2"},tok=A)
call("POST",f"/projects/from-template/{tid}",{"name":"FT"},tok=A); call("DELETE",f"/templates/{ct['id']}",tok=A)
v=call("POST",f"/projects/{P}/versions",{"name":"v1"},tok=A); V=v["id"]
call("POST",pp+"/duplicate",{"name":"AD"},tok=A); call("POST",pp+"/reset",{},tok=A)
v2=call("POST",f"/projects/{P}/versions",{"name":"v2"},tok=A)
for m,x in [("GET",""),("GET",f"/{V}"),("GET",f"/{V}/architecture"),("POST",f"/{V}/restore")]: call(m,f"/projects/{P}/versions"+x,tok=A)
call("POST",f"/projects/{P}/versions/compare",{"fromVersionId":V,"toVersionId":v2["id"]},tok=A)
call("PATCH",f"/projects/{P}/versions/{V}",{"description":"n"},tok=A); call("DELETE",f"/projects/{P}/versions/{v2['id']}",tok=A)
r=call("POST",f"/projects/{P}/reviews",{},tok=A); R=r and r.get("id")
call("GET",f"/projects/{P}/reviews",tok=A)
if R:
  import time; time.sleep(3)
  call("GET",f"/projects/{P}/reviews/{R}",tok=A); f=call("GET",f"/projects/{P}/reviews/{R}/findings",tok=A)
  call("GET",f"/projects/{P}/reviews/{R}/report",tok=A); call("POST",f"/projects/{P}/reviews/{R}/cancel",tok=A,expect_ok=False)
  call("POST",f"/projects/{P}/reviews/compare",{"baseReviewId":R,"targetReviewId":R},tok=A)
  items=f if isinstance(f,list) else (f or {}).get("content",[])
  if items:
    F=items[0]["id"]; call("GET",f"/projects/{P}/reviews/{R}/findings/{F}",tok=A)
    call("PUT",f"/projects/{P}/findings/{F}/decision",{"decision":"ACCEPTED","comment":"ok"},tok=A)
    call("GET",f"/projects/{P}/findings/decisions",tok=A); call("GET",f"/projects/{P}/findings/{F}/history",tok=A)
    call("POST",f"/projects/{P}/findings/{F}/apply",{},tok=A,expect_ok=False); call("DELETE",f"/projects/{P}/findings/{F}/decision",tok=A)
  else: print("NOTE no findings")
b=call("POST",f"/projects/{P}/branches",{"name":"feat"},tok=A); Bi=b["id"]; bp=f"/projects/{P}/branches/{Bi}"
call("GET",f"/projects/{P}/branches",tok=A); call("GET",bp,tok=A); call("PATCH",bp,{"name":"feat2"},tok=A)
ba=call("GET",bp+"/architecture",tok=A); call("PUT",bp+"/architecture",{"document":doc,"expectedRevision":(ba or {}).get("revision")},tok=A)
call("POST",bp+"/compare",{},tok=A); call("POST",bp+"/merge",{},tok=A); call("DELETE",bp,tok=A)
call("GET",f"/projects/{P}/members",tok=A)
i=call("POST",f"/projects/{P}/invitations",{"email":e2,"role":"EDITOR"},tok=A); call("GET",f"/projects/{P}/invitations",tok=A)
call("POST",f"/projects/{P}/invitations/{i['id']}/accept",tok=t2)
i2=call("POST",f"/projects/{P}/invitations",{"email":"z@z.co","role":"VIEWER"},tok=A); call("DELETE",f"/projects/{P}/invitations/{i2['id']}",tok=A)
i3=call("POST",f"/projects/{P}/invitations",{"email":"y@z.co","role":"VIEWER"},tok=A); call("POST",f"/projects/{P}/invitations/{i3['id']}/decline",tok=A,expect_ok=False)
uid2=call("GET","/auth/me",tok=t2); uid2=uid2.get("id") or uid2.get("userId")
call("PATCH",f"/projects/{P}/members/{uid2}",{"role":"VIEWER"},tok=A); call("GET",f"/projects/{P}/permissions",tok=t2)
call("POST",f"/projects/{P}/leave",tok=t2)
call("GET",f"/projects/{P}/presence",tok=A)
for m,x,bd in [("GET","/notifications",None),("GET","/notifications/unread-count",None),("POST","/notifications/mark-all-read",None),("PATCH","/users/me/notification-preferences",{"email":False})]: call(m,x,bd,tok=A)
call("GET","/search?q=P",tok=A); call("GET",f"/projects/{P}/search?q=API",tok=A)
call("GET","/users/me/preferences",tok=A); call("PATCH","/users/me/preferences",{"theme":"dark"},tok=A)
ss=call("GET","/users/me/sessions",tok=A); call("GET","/users/me/connected-accounts",tok=A)
call("DELETE","/users/me/connected-accounts/github",tok=A,expect_ok=False)
for x in ["/health","/info","/metrics"]: call("GET",x,tok=A,base="http://localhost:8081/actuator")
for x in ["/admin/users","/admin/jobs","/admin/audit-events"]: call("GET",x,tok=A)
call("POST","/admin/jobs/"+str(uuid.uuid4())+"/retry",tok=A,expect_ok=False)
# New user-owned AI and commit history routes (test uses configured provider).
k=call("POST","/users/me/ai-keys",{"provider":"OPENAI","label":"Smoke","apiKey":"invalid-smoke-key"},tok=A)
call("GET","/users/me/ai-keys",tok=A)
call("POST",f"/users/me/ai-keys/{k['id']}/test",tok=A)
call("POST",f"/projects/{P}/ai/agent-runs",{"keyId":k['id'],"prompt":"Add a service"},tok=A)
call("GET",f"/projects/{P}/ai/agent-runs",tok=A)
call("GET",f"/projects/{P}/versions?branchId=main",tok=A)
call("GET",f"/projects/{P}/versions/{v['id']}/diff",tok=A)
call("GET","/users/me/invitations",tok=t2)
call("DELETE",f"/users/me/ai-keys/{k['id']}",tok=A)

call("PATCH","/users/me/password",{"currentPassword":"Password123!","newPassword":"Password456!"},tok=A)
call("DELETE",f"/projects/{d['id']}",tok=A); call("POST","/auth/logout",{"refreshToken":t["refreshToken"]},tok=A)
call("GET","/auth/me",tok=A,expect_ok=False)
call("DELETE","/users/me",tok=t2)
