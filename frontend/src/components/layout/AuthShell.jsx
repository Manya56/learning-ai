export default function AuthShell({ eyebrow, title, subtitle, highlights, footer, children }) {
  return (
    <div className="relative min-h-screen overflow-hidden bg-[radial-gradient(circle_at_top,#1b1f3a_0%,#0f0f0f_46%,#0b0b0b_100%)] px-4 py-6 text-(--text) sm:px-6 lg:px-8 lg:py-8">
      <div className="pointer-events-none absolute inset-0 opacity-70">
        <div className="-left-32 absolute top-20 h-72 w-72 rounded-full bg-(--accent)/15 blur-3xl" />
        <div className="-right-24 absolute top-24 h-80 w-80 rounded-full bg-cyan-400/10 blur-3xl" />
        <div className="absolute bottom-0 left-1/2 h-56 w-160 -translate-x-1/2 rounded-full bg-(--accent)/10 blur-3xl" />
      </div>

      <div className="relative mx-auto flex min-h-[calc(100vh-3rem)] max-w-6xl items-center">
        <div className="grid w-full gap-8 lg:grid-cols-[1.05fr_0.95fr] lg:gap-10">
          <section className="flex flex-col justify-center rounded-4xl border border-white/8 bg-white/3 p-6 shadow-[0_30px_90px_rgba(0,0,0,0.24)] backdrop-blur sm:p-8 lg:p-10">
            <div className="inline-flex w-fit items-center rounded-full border border-white/10 bg-white/5 px-4 py-2 text-xs font-medium uppercase tracking-[0.22em] text-(--text-muted)">
              {eyebrow}
            </div>

            <div className="mt-6 max-w-xl">
              <h1 className="text-4xl font-semibold tracking-tight text-white sm:text-5xl">{title}</h1>
              <p className="mt-4 max-w-2xl text-base leading-7 text-(--text-muted) sm:text-lg">{subtitle}</p>
            </div>

            <div className="mt-8 grid gap-4 sm:grid-cols-3 lg:max-w-2xl">
              {highlights.map((item) => (
                <div
                  key={item.title}
                  className="rounded-2xl border border-white/8 bg-[linear-gradient(180deg,rgba(255,255,255,0.05),rgba(255,255,255,0.02))] p-4 shadow-[0_18px_50px_rgba(0,0,0,0.16)]"
                >
                  <div className="mb-4 flex h-11 w-11 items-center justify-center rounded-xl bg-(--accent)/15 text-white ring-1 ring-(--accent)/20">
                    <item.icon className="h-5 w-5" />
                  </div>
                  <h2 className="text-sm font-semibold text-white">{item.title}</h2>
                  <p className="mt-1 text-sm leading-6 text-(--text-muted)">{item.copy}</p>
                </div>
              ))}
            </div>

          </section>

          <section className="flex items-center justify-center">
            <div className="w-full max-w-lg">
              {children}
              {footer ? <div className="mt-4 text-center text-sm text-(--text-muted)">{footer}</div> : null}
            </div>
          </section>
        </div>
      </div>
    </div>
  );
}