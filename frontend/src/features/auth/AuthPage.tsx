import { useState, type FormEvent } from "react";
import { Link, useNavigate } from "@tanstack/react-router";
import { auth } from "@/api/auth";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Check, ArrowLeft } from "lucide-react";
import { BrandLogo } from "@/components/BrandLogo";
type Mode = "login" | "register" | "forgot-password" | "reset-password" | "verify-email";
export function AuthPage({ mode }: { mode: Mode }) {
 const [email, setEmail] = useState(""); const [password, setPassword] = useState(""); const [name, setName] = useState("");
 const [busy, setBusy] = useState(false); const [error, setError] = useState(""); const [message, setMessage] = useState("");
 const navigate = useNavigate();
 const title = { login: "Sign in", register: "Create account", "forgot-password": "Forgot password", "reset-password": "Reset password", "verify-email": "Verify email" }[mode];
 async function submit(e: FormEvent) {
  e.preventDefault(); setBusy(true); setError(""); setMessage("");
  try {
   const token = new URLSearchParams(window.location.search).get("token") || "";
   if (mode === "login") await auth.login(email, password);
   if (mode === "register") await auth.register(email, password, name);
   if (mode === "forgot-password") await auth.forgot(email);
   if (mode === "reset-password" || mode === "verify-email") {
    if (!token) throw new Error("Missing token. Open the link in your email.");
    if (mode === "reset-password") await auth.reset(token, password); else await auth.verify(token);
   }
   if (mode === "login" || mode === "register") void navigate({ to: "/" });
   else setMessage(mode === "forgot-password" ? "If the account exists, a recovery email has been sent." : "Success. You can now log in.");
  } catch(e) { setError(e instanceof Error ? e.message : "Request failed"); } finally { setBusy(false); }
 }
 const subtitle = { login: "Welcome back. Sign in to your workspace.", register: "Start designing systems with your team.", "forgot-password": "We'll email you a link to reset it.", "reset-password": "Choose a new password.", "verify-email": "Confirm your email address." }[mode];
 const field = "h-11 bg-background/60";
 return <main className="grid min-h-screen bg-background lg:grid-cols-2">
  <aside className="relative hidden overflow-hidden border-r border-border bg-[radial-gradient(ellipse_at_top_left,oklch(0.6_0.2_268/0.35),transparent_60%)] p-12 lg:flex lg:flex-col lg:justify-between">
   <Link to="/welcome" aria-label="ArchPilot home" className="inline-flex items-center"><BrandLogo className="h-12 w-auto" /></Link>
   <div className="space-y-6"><h2 className="text-4xl font-bold leading-tight">Design backend systems<br />together, live.</h2>
    <ul className="space-y-3 text-sm text-muted-foreground">{["Live cursors and instant edits with your team", "Senior-level AI architecture review", "Design patterns, simulation and versioning", "Download starter code for every service"].map(t => <li key={t} className="flex items-center gap-2"><Check className="h-4 w-4 text-primary" />{t}</li>)}</ul></div>
   <p className="text-xs text-muted-foreground">Collaborative architecture design</p>
  </aside>
  <section className="flex items-center justify-center p-6"><div className="w-full max-w-sm space-y-6">
   <Link to="/welcome" className="inline-flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground"><ArrowLeft className="h-4 w-4" />Back</Link>
   <div><h1 className="text-2xl font-semibold tracking-tight">{title}</h1><p className="mt-1 text-sm text-muted-foreground">{subtitle}</p></div>
   <form onSubmit={submit} className="space-y-4">
    {mode === "register" && <div className="space-y-1.5"><Label htmlFor="name">Display name</Label><Input id="name" className={field} aria-label="Display name" placeholder="Jane Doe" required maxLength={100} value={name} onChange={e => setName(e.target.value)} /></div>}
    {!["reset-password", "verify-email"].includes(mode) && <div className="space-y-1.5"><Label htmlFor="email">Email</Label><Input id="email" className={field} aria-label="Email" type="email" placeholder="you@company.com" required value={email} onChange={e => setEmail(e.target.value)} /></div>}
    {["login", "register", "reset-password"].includes(mode) && <div className="space-y-1.5"><div className="flex items-center justify-between"><Label htmlFor="password">Password</Label>{mode === "login" && <Link to="/forgot-password" className="text-xs text-primary hover:underline">Forgot password?</Link>}</div><Input id="password" className={field} aria-label="Password" type="password" placeholder={mode === "login" ? "Your password" : "At least 8 characters"} required minLength={mode === "login" ? 1 : 8} maxLength={128} value={password} onChange={e => setPassword(e.target.value)} /></div>}
    <Button disabled={busy} type="submit" className="h-11 w-full">{busy ? "Please wait…" : mode === "login" ? "Sign in" : title}</Button>
   </form>
   {error && <p role="alert" className="rounded-md border border-destructive/40 bg-destructive/10 p-3 text-sm text-destructive">{error}</p>}
   {message && <p role="status" className="rounded-md border border-border bg-card p-3 text-sm">{message}</p>}
   <p className="text-center text-sm text-muted-foreground">{mode === "login" ? <>No account yet? <Link to="/register" className="font-medium text-primary hover:underline">Create one</Link></> : mode === "register" ? <>Already have an account? <Link to="/login" className="font-medium text-primary hover:underline">Sign in</Link></> : <Link to="/login" className="font-medium text-primary hover:underline">Back to sign in</Link>}</p>
   {mode === "login" && <details className="text-xs text-muted-foreground"><summary className="cursor-pointer">Didn't get the verification email?</summary><div className="mt-2 flex gap-2"><Input aria-label="Verification email" type="email" placeholder="Email" value={email} onChange={e => setEmail(e.target.value)} /><Button variant="outline" disabled={busy || !email} onClick={async () => { setBusy(true); try { await auth.resend(email); setMessage("Verification email requested."); } catch(e) { setError(e instanceof Error ? e.message : "Request failed"); } finally { setBusy(false); } }}>Resend</Button></div></details>}
  </div></section>
 </main>;
}
