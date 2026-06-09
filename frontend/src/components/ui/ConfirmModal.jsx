import { AnimatePresence, motion } from "framer-motion";
import Button from "./Button";
import Icon from "./Icon";

// Reusable centered confirm popup. Safe choice on top (stacked, full-width) — mobile-friendly.
export default function ConfirmModal({
  open,
  icon = "help",
  title,
  message,
  confirmLabel = "Confirm",
  confirmVariant = "danger",
  cancelLabel = "Keep going",
  onConfirm,
  onCancel,
}) {
  return (
    <AnimatePresence>
      {open && (
        <div
          className="fixed inset-0 z-[60] flex items-center justify-center bg-black/40 p-4 backdrop-blur-sm"
          onClick={onCancel}
        >
          <motion.div
            initial={{ opacity: 0, scale: 0.96 }}
            animate={{ opacity: 1, scale: 1 }}
            exit={{ opacity: 0, scale: 0.96 }}
            onClick={(e) => e.stopPropagation()}
            className="w-full max-w-sm rounded-2xl border-2 border-[var(--border)] bg-[var(--surface)] p-6 text-center"
          >
            <div className="mx-auto mb-3 flex h-12 w-12 items-center justify-center rounded-2xl bg-[var(--accent-light)]">
              <Icon name={icon} size={26} className="text-[var(--accent)]" />
            </div>
            {title && <h3 className="text-lg font-extrabold tracking-tight">{title}</h3>}
            {message && <p className="mt-2 text-sm font-medium text-[var(--text-muted)]">{message}</p>}
            <div className="mt-5 flex flex-col gap-2">
              <Button variant="secondary" className="w-full active:scale-95" onClick={onCancel}>
                {cancelLabel}
              </Button>
              <Button variant={confirmVariant} className="w-full active:scale-95" onClick={onConfirm}>
                {confirmLabel}
              </Button>
            </div>
          </motion.div>
        </div>
      )}
    </AnimatePresence>
  );
}
