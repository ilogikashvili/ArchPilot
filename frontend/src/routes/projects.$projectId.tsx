import { createFileRoute } from "@tanstack/react-router";
import { AuthGuard } from "@/features/auth/AuthGuard";
import { EditorApp } from "@/app/EditorApp";
export const Route = createFileRoute("/projects/$projectId")({ component: ProjectEditor });
function ProjectEditor() { const { projectId } = Route.useParams(); return <AuthGuard><EditorApp key={projectId} projectId={projectId} /></AuthGuard>; }
