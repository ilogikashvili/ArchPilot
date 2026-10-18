import requests, uuid, json, io, zipfile
B="http://localhost:8081/api/v1"
def h(t): return {"Authorization":"Bearer "+t}
e=f"o{uuid.uuid4().hex[:6]}@t.co"
A=requests.post(B+"/auth/register",json={"email":e,"password":"Password123!","displayName":"Owner"}).json()["accessToken"]
P=requests.post(B+"/projects",json={"name":"Shop"},headers=h(A)).json()["id"]
P2=requests.post(B+"/projects",json={"name":"Other"},headers=h(A)).json()["id"]
r=requests.post(f"{B}/projects/{P}/agents",json={"name":"Claude","provider":"Anthropic","role":"EDITOR"},headers=h(A)); print("create agent",r.status_code); T=r.json()["token"]
print("agent me",requests.get(B+"/agent/me",headers=h(T)).status_code)
ops=[{"op":"node.add","node":{"id":"web","type":"web","label":"Storefront","x":0,"y":0}},
     {"op":"node.add","node":{"id":"gw","type":"api","label":"API Gateway","x":200,"y":0}},
     {"op":"node.add","node":{"id":"orders","type":"service","label":"Order Service","x":400,"y":0,"properties":{"responsibilities":["Create orders"]}}},
     {"op":"node.add","node":{"id":"pay","type":"service","label":"Payment Service","x":400,"y":200}},
     {"op":"node.add","node":{"id":"db","type":"database","label":"Orders DB","x":600,"y":0}},
     {"op":"node.add","node":{"id":"q","type":"queue","label":"Order Events","x":600,"y":200}},
     {"op":"node.add","node":{"id":"c","type":"cache","label":"Session Cache","x":200,"y":200}},
     {"op":"node.add","node":{"id":"stripe","type":"external","label":"Stripe","x":600,"y":400}},
     {"op":"edge.add","edge":{"id":"e1","sourceId":"web","targetId":"gw","type":"sync","label":"HTTPS"}},
     {"op":"edge.add","edge":{"id":"e2","sourceId":"gw","targetId":"orders","type":"sync","label":"REST"}},
     {"op":"edge.add","edge":{"id":"e3","sourceId":"orders","targetId":"db","type":"sync"}},
     {"op":"edge.add","edge":{"id":"e4","sourceId":"orders","targetId":"q","type":"async"}},
     {"op":"edge.add","edge":{"id":"e5","sourceId":"q","targetId":"pay","type":"event"}},
     {"op":"edge.add","edge":{"id":"e6","sourceId":"pay","targetId":"stripe","type":"sync"}},
     {"op":"edge.add","edge":{"id":"e7","sourceId":"gw","targetId":"c","type":"sync"}}]
r=requests.post(f"{B}/projects/{P}/architecture/operations",json={"clientId":"agent","operations":ops},headers=h(T)); print("agent ops",r.status_code,r.json().get("revision"))
r=requests.post(f"{B}/projects/{P}/architecture/operations",json={"operations":[{"op":"node.update","id":"orders","patch":{"x":420}}]},headers=h(A)); print("user ops",r.status_code,r.json().get("revision"))
print("agent other project (expect 403)",requests.get(f"{B}/projects/{P2}",headers=h(T)).status_code)
print("agent manage agents (expect 403)",requests.get(f"{B}/projects/{P}/agents",headers=h(T)).status_code)
print("agent list projects (expect 403)",requests.get(f"{B}/projects",headers=h(T)).status_code)
print("bad op (expect 400/422)",requests.post(f"{B}/projects/{P}/architecture/operations",json={"operations":[{"op":"edge.add","edge":{"id":"x","sourceId":"nope","targetId":"db"}}]},headers=h(T)).status_code)
print("presence",requests.get(f"{B}/projects/{P}/presence",headers=h(A)).status_code)
for lay in ["multi-module","per-service"]:
  r=requests.get(f"{B}/projects/{P}/codegen?layout={lay}&groupId=com.shop",headers=h(A)); print("codegen",lay,r.status_code,r.headers.get("content-disposition"))
  z=zipfile.ZipFile(io.BytesIO(r.content)); z.extractall(f"/tmp/gen-{lay}"); print(len(z.namelist()),"files")
ags=requests.get(f"{B}/projects/{P}/agents",headers=h(A)).json();
print("revoke",requests.delete(f"{B}/projects/{P}/agents/{ags[0]['id']}",headers=h(A)).status_code)
print("revoked token (expect 401)",requests.get(B+"/agent/me",headers=h(T)).status_code)
