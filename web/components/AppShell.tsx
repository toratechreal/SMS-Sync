"use client"

import { usePathname } from "next/navigation"
import { Sidebar } from "./Sidebar"

export function AppShell({ children }: { children: React.ReactNode }) {
  const pathname = usePathname()
  const isHome = pathname === "/"

  return (
    <div className="flex h-screen overflow-hidden">
      {/* Sidebar: always on desktop, only on the home route on mobile */}
      <div className={(isHome ? "flex" : "hidden") + " w-full md:flex md:w-auto"}>
        <Sidebar />
      </div>

      {/* Main pane: always on desktop, only off the home route on mobile */}
      <main className={(isHome ? "hidden" : "flex") + " min-w-0 flex-1 md:flex"}>
        {children}
      </main>
    </div>
  )
}
