import {createFileRoute} from '@tanstack/react-router';
import {AuthGuard} from '@/features/auth/AuthGuard';
import {AppShell} from '@/features/workspace/AppShell';
import {SettingsPage} from '@/features/workspace/Pages';
export const Route=createFileRoute('/settings')({component:()=> <AuthGuard><AppShell><SettingsPage/></AppShell></AuthGuard>});
