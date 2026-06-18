import type { Metadata } from "next"
import "./globals.css"
import { AppShell } from "@/components/AppShell"

export const metadata: Metadata = {
  title: "Messages",
  description: "Read and reply to your synced SMS from the browser.",
}

export default function RootLayout({
  children,
}: {
  children: React.ReactNode
}) {
  return (
    <html lang="en">
      <body className="bg-white text-neutral-900 dark:bg-neutral-950 dark:text-neutral-100">
        <AppShell>{children}</AppShell>
      </body>
    </html>
  )
}
