import {createFileRoute} from '@tanstack/react-router';
import {AuthGuard} from '@/features/auth/AuthGuard';
import {AppShell} from '@/features/workspace/AppShell';
import {CollectionPage} from '@/features/workspace/Pages';
export const Route=createFileRoute('/search')({component:()=> <AuthGuard><AppShell><CollectionPage kind="search"/></AppShell></AuthGuard>});
