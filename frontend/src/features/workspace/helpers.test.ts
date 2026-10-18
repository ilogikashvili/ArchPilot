import {describe,it,expect} from 'vitest';
import {diffCounts,initials,relativeTime} from './helpers';
describe('commit helpers',()=>{it('counts nodes and edges',()=>expect(diffCounts({nodes:{added:[{id:'a'}]},edges:{removed:[{id:'b'}],changed:[{}]}})).toEqual({added:1,removed:1,changed:1}));it('handles empty diffs',()=>expect(diffCounts({}).added).toBe(0));it('formats author and time',()=>{expect(initials('Ada Lovelace')).toBe('AL');expect(relativeTime('2025-01-01T00:00:00Z',Date.parse('2025-01-01T00:02:00Z'))).toBe('2m ago');});});

import { diffCounts as dc } from './helpers';
import { it as it2, expect as ex2 } from 'vitest';
it2('counts server change summaries given as numbers', () => {
  ex2(dc({ nodes: { added: 2, removed: 1, changed: 0 }, edges: { added: 1, removed: 0, changed: 3 } } as any)).toEqual({ added: 3, removed: 1, changed: 3 });
});
