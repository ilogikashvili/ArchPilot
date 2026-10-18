export type DiffItem = {id?:string;label?:string;before?:unknown;after?:unknown};
export type Diff = {nodes?:Partial<Record<'added'|'removed'|'changed',DiffItem[]>>;edges?:Partial<Record<'added'|'removed'|'changed',DiffItem[]>>};
const n=(v:unknown)=>typeof v==='number'?v:Array.isArray(v)?v.length:0;
export function diffCounts(diff:Diff){const sum=(key:'added'|'removed'|'changed')=>n((diff.nodes as any)?.[key])+n((diff.edges as any)?.[key]);return {added:sum('added'),removed:sum('removed'),changed:sum('changed')};}
export function initials(name:string){return name.trim().split(/\s+/).slice(0,2).map(s=>s[0]?.toUpperCase()).join('')||'?';}
export function relativeTime(date:string,now=Date.now()){const seconds=Math.max(0,Math.floor((now-Date.parse(date))/1000));if(!Number.isFinite(seconds))return 'Unknown time';if(seconds<60)return 'just now';if(seconds<3600)return `${Math.floor(seconds/60)}m ago`;if(seconds<86400)return `${Math.floor(seconds/3600)}h ago`;return `${Math.floor(seconds/86400)}d ago`;}
