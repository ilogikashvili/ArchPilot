import {AppShell} from "@/features/workspace/AppShell";
import { createFileRoute } from "@tanstack/react-router";
import { AuthGuard } from "@/features/auth/AuthGuard";
import { ProjectsPage } from "@/features/projects/ProjectsPage";
export const Route = createFileRoute("/")({ component: () => <AuthGuard><AppShell><ProjectsPage /></AppShell></AuthGuard> });
