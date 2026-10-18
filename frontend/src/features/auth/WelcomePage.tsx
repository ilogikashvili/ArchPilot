import { Link } from "@tanstack/react-router";
import {
  Activity,
  ArrowRight,
  Bot,
  Boxes,
  Check,
  Cpu,
  Download,
  GitBranch,
  Layers,
  MessagesSquare,
  Search,
  ShieldCheck,
  Sparkles,
  Users,
} from "lucide-react";
import { BrandLogo } from "@/components/BrandLogo";

const FEATURES = [
  {
    icon: Boxes,
    title: "Design visually",
    text: "Build your system on an open canvas. Drag in services, APIs, databases, caches, message queues, gateways, load balancers, and external systems, then connect them to show how data moves. Click any part to name it, describe what it does, and set its details. Start from a blank canvas or a template, and undo or redo anything.",
  },
  {
    icon: Users,
    title: "Work together in real time",
    text: "Everyone sees the same canvas at the same time: teammates' cursors, selections, and drags appear as they happen. Invite people by email and choose whether they can manage the project, edit the design, or view and comment. Invitations arrive in each person's inbox to accept or decline.",
  },
  {
    icon: GitBranch,
    title: "Version history, like GitHub for diagrams",
    text: "Save versions with a message describing what changed. See who changed what and when, with the additions, removals, and modifications. Branch to experiment safely, compare with the original, merge when ready, or restore an earlier version.",
  },
  {
    icon: ShieldCheck,
    title: "AI review from a senior architect",
    text: "Ask for a critical, honest review of your design. Each finding is ranked by severity and includes a clear explanation and concrete fix, alongside an overall verdict on whether the design is ready.",
    details: [
      "Security gaps and missing access checks",
      "Single points of failure and tightly coupled services",
      "Missing caching, queues, retries, or fallbacks",
      "Shared databases, layering issues, and poor separation of concerns",
      "Missing monitoring, logging, and health checks",
    ],
  },
  {
    icon: Layers,
    title: "Apply design patterns in one click",
    text: "Preview a proven pattern, then apply it and watch your design transform. You can also apply any of the SOLID principles—or all five together.",
    details: [
      "API Gateway and Backend for Frontend",
      "Database per Service and Cache-Aside",
      "CQRS, Event-Driven, and Saga",
      "Circuit Breaker",
      "Load Balancer + Replicas",
      "Single Responsibility, Open/Closed, Liskov Substitution, Interface Segregation, and Dependency Inversion",
    ],
  },
  {
    icon: Bot,
    title: "AI teammates and team chat",
    text: "Add up to 12 AI agents with roles like architect, security expert, DevOps engineer, database specialist, or a custom role. Mention an agent with @, let agents reply to each other, or start a multi-round group discussion that your team can watch and join. Stop a discussion at any time. External AI agents and tools can also connect with secure, project-only access keys.",
  },
  {
    icon: Cpu,
    title: "Use any AI, including your own local models",
    text: "Connect a cloud AI provider using your own key, or use Ollama with a model such as Qwen or Llama. With Ollama, your backend must be configured to reach the machine running the model.",
  },
  {
    icon: Activity,
    title: "See requests flow through your system",
    text: "Send a simulated request into your design and watch it travel step by step—from the client through gateways, services, caches, queues, and databases, then back. Inspect what goes in and comes out at each stop to spot slow paths, missing links, and weak points before you build.",
  },
  {
    icon: Download,
    title: "From diagram to code",
    text: "Choose Download structure to generate a project with folders, files, and Java Spring Boot starter code for every service in your design. Open it in your editor and start building from a structure that matches your architecture. Support for more languages is coming.",
  },
];

const AUDIENCES = [
  {
    title: "Students and junior developers",
    text: "Learn how real systems are designed and explore the trade-offs behind architecture decisions.",
  },
  {
    title: "Senior engineers and architects",
    text: "Share designs with your team and catch risks before implementation begins.",
  },
  {
    title: "Startups",
    text: "Plan a backend together before writing the first line of code.",
  },
  {
    title: "Teams",
    text: "Keep one shared, versioned source of truth for your software architecture.",
  },
];

const STEPS = [
  "Create a project and invite your team",
  "Design your system on the shared canvas",
  "Review it with the AI senior architect and your AI teammates",
  "Improve it with design patterns and SOLID principles",
  "Simulate requests to check the flow",
  "Save versions and branch to try ideas safely",
  "Download your starter code and start building",
];

const TOOLS = [
  "Search across projects",
  "Notifications for invitations, reviews, and changes",
  "Ready-made templates",
  "Personal settings and AI connections",
  "Admin tools for managing users",
  "Panels that keep their content when you switch, so chats and reviews stay put",
];

const STACK = [
  "React",
  "TypeScript",
  "Vite",
  "Java",
  "Spring Boot",
  "WebSockets",
  "PostgreSQL / H2",
  "Cloud AI providers",
  "Ollama",
];

export function WelcomePage() {
  return (
    <div className="min-h-screen bg-background text-foreground">
      <div className="pointer-events-none fixed inset-0 bg-[radial-gradient(ellipse_at_top,oklch(0.6_0.2_268/0.25),transparent_60%)]" />
      <header className="relative mx-auto flex max-w-6xl items-center justify-between px-6 py-5">
        <BrandLogo className="h-10 w-auto" />
        <nav className="flex items-center gap-2 text-sm" aria-label="Account">
          <Link
            to="/login"
            className="rounded-md px-4 py-2 text-muted-foreground hover:text-foreground"
          >
            Sign in
          </Link>
          <Link
            to="/register"
            className="rounded-md bg-primary px-4 py-2 font-medium text-primary-foreground hover:opacity-90"
          >
            Get started
          </Link>
        </nav>
      </header>

      <main className="relative">
        <section className="mx-auto max-w-4xl px-6 pb-20 pt-16 text-center sm:pt-24">
          <span className="inline-flex items-center gap-2 rounded-full border border-border bg-card/60 px-3 py-1 text-xs text-muted-foreground">
            <Sparkles className="h-3.5 w-3.5 text-primary" />A shared workspace
            for system design
          </span>
          <h1 className="mt-6 text-4xl font-bold tracking-tight sm:text-6xl">
            ArchMain
            <span className="mt-3 block bg-gradient-to-r from-primary to-fuchsia-400 bg-clip-text text-3xl text-transparent sm:text-5xl">
              Design, review, and build backend systems together, live.
            </span>
          </h1>
          <p className="mx-auto mt-6 max-w-3xl text-lg leading-relaxed text-muted-foreground">
            ArchMain is a shared workspace where developers, architects, and AI
            teammates design software systems on one canvas. Sketch your
            architecture, get it reviewed like a senior engineer would, apply
            proven design patterns in one click, test how requests flow, track
            every version, and download ready-to-run starter code.
          </p>
          <div className="mt-8 flex flex-wrap justify-center gap-3">
            <Link
              to="/register"
              className="inline-flex items-center gap-2 rounded-lg bg-primary px-6 py-3 font-medium text-primary-foreground hover:opacity-90"
            >
              Get started — it&apos;s free
              <ArrowRight className="h-4 w-4" />
            </Link>
            <Link
              to="/login"
              className="rounded-lg border border-border bg-card/60 px-6 py-3 font-medium hover:bg-accent"
            >
              Sign in
            </Link>
          </div>
        </section>

        <section
          id="why-archmain"
          className="mx-auto max-w-5xl px-6 pb-20"
          aria-labelledby="why-heading"
        >
          <div className="rounded-2xl border border-border bg-card/60 p-6 sm:p-10">
            <p className="text-sm font-medium text-primary">Why ArchMain?</p>
            <h2
              id="why-heading"
              className="mt-2 text-2xl font-semibold tracking-tight sm:text-3xl"
            >
              From scattered diagrams to a design your team can build.
            </h2>
            <p className="mt-4 leading-relaxed text-muted-foreground">
              System designs often end up scattered across whiteboard photos,
              outdated diagrams, and long chat threads. Problems like a single
              database for everything, no caching, or one service that can bring
              the whole system down are often found only after the code is
              written.
            </p>
            <p className="mt-4 leading-relaxed text-muted-foreground">
              ArchMain puts the process in one place. Your team designs together
              in real time, AI helps check the design before anyone writes code,
              and a ready design can become a project structure you can start
              building from.
            </p>
          </div>
        </section>

        <section
          id="features"
          className="mx-auto max-w-6xl px-6 pb-20"
          aria-labelledby="features-heading"
        >
          <div className="mx-auto mb-10 max-w-2xl text-center">
            <h2
              id="features-heading"
              className="text-2xl font-semibold tracking-tight sm:text-3xl"
            >
              Design with your whole team, from first sketch to starter code
            </h2>
            <p className="mt-3 text-muted-foreground">
              Everything you need to explore, review, and improve a system
              before you build it.
            </p>
          </div>
          <div className="grid gap-4 md:grid-cols-2">
            {FEATURES.map(({ icon: Icon, title, text, details }) => (
              <article
                key={title}
                className="rounded-xl border border-border bg-card/60 p-5 backdrop-blur transition-colors hover:border-primary/50 sm:p-6"
              >
                <Icon className="h-5 w-5 text-primary" aria-hidden="true" />
                <h3 className="mt-3 font-semibold">{title}</h3>
                <p className="mt-2 text-sm leading-relaxed text-muted-foreground">
                  {text}
                </p>
                {details && (
                  <ul className="mt-4 space-y-2">
                    {details.map((detail) => (
                      <li
                        key={detail}
                        className="flex items-start gap-2 text-sm text-muted-foreground"
                      >
                        <Check
                          className="mt-0.5 h-4 w-4 shrink-0 text-primary"
                          aria-hidden="true"
                        />
                        <span>{detail}</span>
                      </li>
                    ))}
                  </ul>
                )}
              </article>
            ))}
          </div>
        </section>

        <section
          id="workspace-tools"
          className="mx-auto max-w-6xl px-6 pb-20"
          aria-labelledby="tools-heading"
        >
          <div className="rounded-2xl border border-border bg-card/60 p-6 sm:p-10">
            <div className="flex items-center gap-3">
              <Search className="h-5 w-5 text-primary" aria-hidden="true" />
              <h2
                id="tools-heading"
                className="text-xl font-semibold tracking-tight sm:text-2xl"
              >
                Everything you need in one place
              </h2>
            </div>
            <ul className="mt-6 grid gap-3 sm:grid-cols-2">
              {TOOLS.map((tool) => (
                <li
                  key={tool}
                  className="flex items-start gap-2 text-sm text-muted-foreground"
                >
                  <Check
                    className="mt-0.5 h-4 w-4 shrink-0 text-primary"
                    aria-hidden="true"
                  />
                  <span>{tool}</span>
                </li>
              ))}
            </ul>
          </div>
        </section>

        <section
          id="audience"
          className="mx-auto max-w-6xl px-6 pb-20"
          aria-labelledby="audience-heading"
        >
          <div className="mx-auto mb-8 max-w-2xl text-center">
            <h2
              id="audience-heading"
              className="text-2xl font-semibold tracking-tight sm:text-3xl"
            >
              Who it&apos;s for
            </h2>
            <p className="mt-3 text-muted-foreground">
              A better place to learn, plan, and agree on how a system should
              work.
            </p>
          </div>
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
            {AUDIENCES.map(({ title, text }) => (
              <article
                key={title}
                className="rounded-xl border border-border bg-card/60 p-5"
              >
                <h3 className="font-semibold">{title}</h3>
                <p className="mt-2 text-sm leading-relaxed text-muted-foreground">
                  {text}
                </p>
              </article>
            ))}
          </div>
        </section>

        <section
          id="how-it-works"
          className="mx-auto max-w-5xl px-6 pb-20"
          aria-labelledby="how-heading"
        >
          <div className="mx-auto mb-8 max-w-2xl text-center">
            <h2
              id="how-heading"
              className="text-2xl font-semibold tracking-tight sm:text-3xl"
            >
              How it works
            </h2>
            <p className="mt-3 text-muted-foreground">
              Go from an empty canvas to a plan your team can start building.
            </p>
          </div>
          <ol className="grid gap-3 sm:grid-cols-2">
            {STEPS.map((step, index) => (
              <li
                key={step}
                className="flex items-center gap-4 rounded-xl border border-border bg-card/60 p-4"
              >
                <span className="grid h-9 w-9 shrink-0 place-items-center rounded-full bg-primary/20 text-sm font-semibold text-primary">
                  {index + 1}
                </span>
                <span className="text-sm font-medium">{step}</span>
              </li>
            ))}
          </ol>
        </section>

        <section className="mx-auto max-w-4xl px-6 pb-20 text-center">
          <h2 className="flex items-center justify-center gap-2 text-lg font-semibold">
            <MessagesSquare
              className="h-5 w-5 text-primary"
              aria-hidden="true"
            />
            Built with
          </h2>
          <div className="mt-4 flex flex-wrap justify-center gap-2">
            {STACK.map((technology) => (
              <span
                key={technology}
                className="rounded-full border border-border px-3 py-1 text-xs text-muted-foreground"
              >
                {technology}
              </span>
            ))}
          </div>
        </section>

        <section className="mx-auto max-w-5xl px-6 pb-24">
          <div className="rounded-2xl border border-primary/30 bg-card/70 px-6 py-10 text-center sm:px-10">
            <h2 className="text-2xl font-semibold tracking-tight sm:text-3xl">
              Ready to design your next system?
            </h2>
            <p className="mx-auto mt-3 max-w-xl text-muted-foreground">
              Create your free account and start building with your team today.
            </p>
            <Link
              to="/register"
              className="mt-6 inline-flex items-center gap-2 rounded-lg bg-primary px-6 py-3 font-medium text-primary-foreground hover:opacity-90"
            >
              Get started — it&apos;s free
              <ArrowRight className="h-4 w-4" />
            </Link>
          </div>
        </section>
      </main>

      <footer className="relative border-t border-border py-6 text-center text-xs text-muted-foreground">
        ArchMain · collaborative architecture design
      </footer>
    </div>
  );
}
