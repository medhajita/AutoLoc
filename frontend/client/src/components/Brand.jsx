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
