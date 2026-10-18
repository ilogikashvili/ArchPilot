import { createFileRoute } from "@tanstack/react-router";
import { AuthPage } from "@/features/auth/AuthPage";
export const Route = createFileRoute("/reset-password")({ component: () => <AuthPage mode="reset-password" /> });
