export function Logo({ onClick }) {
  return (
    <a
      href="#/"
      className="logo"
      onClick={(e) => {
        e.preventDefault()
        onClick?.()
      }}
    >
      Auto<span>Loc</span>
    </a>
  )
}

export function CarIcon({ width = 200 }) {
  return (
    <svg
      viewBox="0 0 200 80"
      width={width}
      height={(width * 80) / 200}
      fill="none"
      stroke="currentColor"
      strokeWidth="3"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
    >
      <path d="M20 56h-6a4 4 0 0 1-4-4v-8a6 6 0 0 1 5-6l22-5 18-18h58l24 18 22 4a6 6 0 0 1 5 6v9a4 4 0 0 1-4 4h-8" />
      <path d="M52 34h86" />
      <circle cx="52" cy="58" r="10" />
      <circle cx="148" cy="58" r="10" />
      <path d="M62 56h76" />
    </svg>
  )
}
