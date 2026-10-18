import type { ButtonHTMLAttributes, ReactNode } from "react";

type BtnProps = ButtonHTMLAttributes<HTMLButtonElement> & { label: string; children: ReactNode };

export function IconButton({ label, children, className = "", ...rest }: BtnProps) {
  return (
    <button
      type="button"
      aria-label={label}
      title={label}
      className={`grid h-8 w-8 place-items-center rounded-md text-muted-foreground transition hover:bg-accent hover:text-foreground focus-visible:outline-2 focus-visible:outline-ring disabled:pointer-events-none disabled:opacity-35 [&_svg]:h-4 [&_svg]:w-4 ${className}`}
      {...rest}
    >
      {children}
    </button>
  );
}

export function ToolbarButton({ label, children, className = "", ...rest }: BtnProps) {
  return (
    <button
      type="button"
      title={label}
      aria-label={label}
      className={`inline-flex h-8 items-center gap-1.5 rounded-md px-2 text-xs text-muted-foreground transition hover:bg-accent hover:text-foreground focus-visible:outline-2 focus-visible:outline-ring disabled:pointer-events-none disabled:opacity-35 [&_svg]:h-3.5 [&_svg]:w-3.5 ${className}`}
      {...rest}
    >
      {children}
    </button>
  );
}

export function SectionLabel({ children, className = "" }: { children: ReactNode; className?: string }) {
  return <div className={`px-2 text-[10px] font-semibold uppercase tracking-[0.12em] text-muted-foreground ${className}`}>{children}</div>;
}

export const fieldClass =
  "w-full rounded-md border border-input bg-background/60 px-2.5 py-1.5 text-xs text-foreground placeholder:text-muted-foreground focus:border-ring focus:outline-none focus:ring-2 focus:ring-ring/30";
