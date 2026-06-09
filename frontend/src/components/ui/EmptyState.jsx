import Icon from "./Icon";

// Shared empty-state block: icon + title + message + optional action.
// iconClassName lets positive states use the accent color; neutral lists stay muted.
export default function EmptyState({
  icon = "inbox",
  iconClassName = "text-[var(--text-muted)]",
  iconSize = 32,
  title,
  message,
  action,
  dashed = false,
  className = "",
}) {
  return (
    <div
      className={`flex flex-col items-center justify-center gap-2 py-8 text-center ${
        dashed ? "rounded-2xl border-2 border-dashed border-[var(--border)] bg-[var(--surface-2)]" : ""
      } ${className}`}
    >
      <Icon name={icon} size={iconSize} className={iconClassName} />
      {title ? <p className="text-sm font-semibold text-[var(--text)]">{title}</p> : null}
      {message ? <p className="max-w-xs text-xs text-[var(--text-muted)]">{message}</p> : null}
      {action ? <div className="mt-2">{action}</div> : null}
    </div>
  );
}
