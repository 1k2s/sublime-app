// Losangos de enfeite da casca: no pé do menu (placement "sidebar") e nos cantos de baixo do
// painel do conteúdo ("contentStart", junto do menu, e "contentEnd"). Só enfeite: o leitor de
// tela e o mouse passam direto. Quem usa define a posição e a largura pelo className
import styles from "./BackgroundShapes.module.css";

const ANGLE = 38;

// Quadrado girado em volta do próprio centro
function Square({ cx, cy, size, className }) {
  return (
    <rect
      className={className}
      x={cx - size / 2}
      y={cy - size / 2}
      width={size}
      height={size}
      transform={`rotate(${ANGLE} ${cx} ${cy})`}
    />
  );
}

const SHAPES = {
  sidebar: {
    viewBox: "0 0 480 396",
    content: (
      <>
        <polygon className={styles.band} points="395,118 900,530 900,900 35,598" />
        <polygon className={styles.outlineOnShell} points="320,62 430,150 342,262 232,172" />
      </>
    ),
  },
  contentStart: {
    viewBox: "480 0 360 396",
    content: (
      <>
        <polygon className={styles.sage} points="480,150 1080,642 1080,1000 480,1000" />
        <polygon className={styles.dark} points="440,226 610,355 520,475 350,346" />
        <polygon className={styles.sand} points="730,285 775,322 737,365 692,330" />
        <polygon className={styles.outline} points="628,168 750,262 660,382 535,288" />
      </>
    ),
  },
  contentEnd: {
    viewBox: "0 0 804 636",
    content: (
      <>
        <Square className={styles.cream} cx={210} cy={748} size={400} />
        <Square className={styles.sage} cx={612} cy={654} size={600} />
        <Square className={styles.dark} cx={716} cy={241} size={298} />
        <Square className={styles.green} cx={394} cy={592} size={260} />
        <Square className={styles.sand} cx={245} cy={386} size={110} />
        <Square className={styles.outline} cx={514} cy={341} size={303} />
      </>
    ),
  },
};

export function BackgroundShapes({ placement, className }) {
  const { viewBox, content } = SHAPES[placement];

  return (
    <svg
      className={[styles.shapes, className].filter(Boolean).join(" ")}
      viewBox={viewBox}
      aria-hidden="true"
      focusable="false"
    >
      {content}
    </svg>
  );
}
