import {createFileRoute} from '@tanstack/react-router';
import {AuthGuard} from '@/features/auth/AuthGuard';
import {AppShell} from '@/features/workspace/AppShell';
import {CollectionPage} from '@/features/workspace/Pages';
export const Route=createFileRoute('/templates')({component:()=> <AuthGuard><AppShell><CollectionPage kind="templates"/></AppShell></AuthGuard>});
