// Formas de enfeite da casca, em lâminas curvas: no pé do menu (placement "sidebar") e nos cantos
// do painel do conteúdo ("contentStart", no alto junto do menu, e "contentEnd", embaixo à direita).
// Só enfeite: o leitor de tela e o mouse passam direto. Quem usa define a posição e a largura
// pelo className; a altura sai da proporção do viewBox
import styles from "./BackgroundShapes.module.css";

const SHAPES = {
  // Faixa azul saindo da borda esquerda e uma lâmina verde clara ao lado
  sidebar: {
    viewBox: "0 456 162 120",
    content: (
      <>
        <path className={styles.greenPale} d="M27,509 C70,507 128,540 152,576 L120,576 C106,550 70,518 27,509 Z" />
        <path className={styles.blue} d="M0,511 C28,516 70,538 87,576 L117,576 C100,548 55,528 0,525 Z" />
      </>
    ),
  },
  // Canto de cima do painel: uma cunha verde na quina e uma lâmina azulada bem clara cruzando
  contentStart: {
    viewBox: "0 0 97 40",
    content: (
      <>
        <path className={styles.bluePale} d="M15,31 C40,26 66,12 84,0 L74,0 C56,12 36,22 15,31 Z" />
        <path className={styles.green} d="M0,0 L39,0 C28,14 14,22 0,27 Z" />
      </>
    ),
  },
  // Canto de baixo à direita: duas lâminas verdes encaixadas
  contentEnd: {
    viewBox: "0 0 91 106",
    content: (
      <>
        <path className={styles.greenSheer} d="M91,8 C70,20 32,52 2,106 L58,106 C66,86 78,70 91,62 Z" />
        <path className={styles.greenSoft} d="M91,37 C64,46 34,74 14,106 L50,106 C58,92 72,76 91,66 Z" />
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
