import { avatarColor, initial } from "@/lib/display"

export function Avatar({ address, size = 40 }: { address: string; size?: number }) {
  return (
    <div
      className="flex shrink-0 items-center justify-center rounded-full font-medium text-white select-none"
      style={{
        width: size,
        height: size,
        backgroundColor: avatarColor(address),
        fontSize: size * 0.42,
      }}
      aria-hidden
    >
      {initial(address)}
    </div>
  )
}
