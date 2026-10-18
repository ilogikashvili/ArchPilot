type BrandLogoProps = {
  compact?: boolean;
  className?: string;
};

export function BrandLogo({ compact = false, className }: BrandLogoProps) {
  return (
    <img
      src={compact ? "/archpilot-mark.png" : "/archpilot-logo.png"}
      alt="ArchPilot"
      className={className ?? (compact ? "h-8 w-8" : "h-10 w-auto")}
    />
  );
}
