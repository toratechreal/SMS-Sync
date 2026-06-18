import Link from "next/link"

/** Scrollable right-pane wrapper for secondary pages (settings, status). */
export function PagePane({
  title,
  children,
}: {
  title: string
  children: React.ReactNode
}) {
  return (
    <div className="flex h-full w-full flex-col bg-white dark:bg-neutral-950">
      <header className="flex items-center gap-2 border-b border-neutral-200 px-4 py-3 dark:border-neutral-800">
        <Link
          href="/"
          className="-ml-1 rounded-full p-1.5 text-neutral-600 hover:bg-neutral-100 md:hidden dark:text-neutral-300 dark:hover:bg-neutral-800"
          aria-label="Back"
        >
          <svg width="24" height="24" viewBox="0 0 24 24" fill="currentColor" aria-hidden>
            <path d="M20 11H7.83l5.59-5.59L12 4l-8 8 8 8 1.41-1.41L7.83 13H20z" />
          </svg>
        </Link>
        <h1 className="text-base font-medium text-neutral-900 dark:text-neutral-100">
          {title}
        </h1>
      </header>
      <div className="flex-1 overflow-y-auto px-5 py-5">
        <div className="mx-auto max-w-2xl">{children}</div>
      </div>
    </div>
  )
}
