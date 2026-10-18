import { useEffect, useState, type ReactNode } from "react";
import { useNavigate } from "@tanstack/react-router";
import { auth, type User } from "@/api/auth";
import { Button } from "@/components/ui/button";
export function AuthGuard({ children }: { children: ReactNode }) {
 const navigate = useNavigate();
 const [user, setUser] = useState<User | null>(null);
 const [error, setError] = useState("");
 useEffect(() => {
  let active = true;
  const login = () => { void navigate({ to: "/welcome" }); };
  window.addEventListener("auth:logout", login);
  if (!localStorage.getItem("accessToken") && !localStorage.getItem("refreshToken")) login();
  else auth.me().then(u => { if (active) setUser(u); }).catch(e => { if (active) setError(e.message); });
  return () => { active = false; window.removeEventListener("auth:logout", login); };
 }, [navigate]);
 if (!user) return <main className="p-8">{error ? <div role="alert">{error} <Button onClick={() => window.location.reload()}>Retry</Button></div> : "Loading account…"}</main>;
 return <>{children}</>;
}
export function AccountMenu() {
 const [user, setUser] = useState<User | null>(null);
 const [error, setError] = useState("");
 const navigate = useNavigate();
 useEffect(() => { auth.me().then(setUser).catch(e => setError(e.message)); }, []);
 return <div className="flex items-center gap-2 text-xs"><span>{user?.displayName ?? error}</span><Button size="sm" variant="ghost" onClick={async () => { try { await auth.logout(); } catch { /* Local logout still completes. */ } void navigate({ to: "/welcome" }); }}>Logout</Button></div>;
}
