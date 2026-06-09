// Thin wrapper around Google Material Symbols (Rounded).
// Usage: <Icon name="dashboard" size={16} /> — color is inherited via currentColor.
// `fill`, `weight`, and `grade` map to the variable-font axes.
export default function Icon({
  name,
  size = 20,
  weight = 500,
  fill = 0,
  grade = 0,
  className = "",
  style,
  ...props
}) {
  return (
    <span
      className={`material-symbols-rounded select-none ${className}`}
      style={{
        fontSize: size,
        width: size,
        height: size,
        lineHeight: 1,
        display: "inline-flex",
        alignItems: "center",
        justifyContent: "center",
        fontVariationSettings: `'FILL' ${fill}, 'wght' ${weight}, 'GRAD' ${grade}, 'opsz' ${Math.max(20, Math.min(48, size))}`,
        ...style,
      }}
      aria-hidden="true"
      {...props}
    >
      {name}
    </span>
  );
}
