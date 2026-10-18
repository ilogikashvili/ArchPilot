import { createFileRoute } from "@tanstack/react-router";
import { WelcomePage } from "@/features/auth/WelcomePage";
export const Route = createFileRoute("/welcome")({ component: WelcomePage });
