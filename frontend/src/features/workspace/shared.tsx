import { useEffect, useState, type ReactNode } from 'react';
import { api } from '@/api/client';
import { Button } from '@/components/ui/button';
export const rows = <T,>(value: T[] | {content:T[]}): T[] => Array.isArray(value) ? value : value.content;
export function useResource<T>(path: string) {
 const [data,setData]=useState<T>(); const [error,setError]=useState(''); const [loading,setLoading]=useState(true);
 async function reload(){if(path.includes('//')){setLoading(false);return;}setLoading(true);setError('');try{setData(await api<T>(path));}catch(e){setError(e instanceof Error?e.message:'Request failed');}finally{setLoading(false);}}
 useEffect(()=>{void reload();},[path]);
 return {data,error,loading,reload};
}
export function ResourceState({resource,empty=false}:{resource:{loading:boolean;error:string;reload:()=>unknown};empty?:boolean}) {return resource.loading?<p className="text-sm text-muted-foreground">Loading…</p>:resource.error?<div role="alert" className="text-sm text-destructive">{resource.error} <Button variant="outline" size="sm" onClick={()=>void resource.reload()}>Retry</Button></div>:empty?<p className="text-sm text-muted-foreground">Nothing here yet.</p>:null;}
export function Box({title,children}:{title:string;children:ReactNode}){return <section className="space-y-4 rounded-xl border border-border bg-card p-5"><h2 className="font-semibold">{title}</h2>{children}</section>;}
