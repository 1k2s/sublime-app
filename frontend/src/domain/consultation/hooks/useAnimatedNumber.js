// Um número que anda do valor anterior até o novo (o total no centro da pizza sobe do zero ao
// abrir e desliza ao trocar de período). Só efeito visual: o último quadro é o valor exato da
// API, sem conta. Com prefers-reduced-motion, troca direto
import { useEffect, useRef, useState } from "react";

const DURATION_MS = 900;

function prefersReducedMotion() {
  return window.matchMedia?.("(prefers-reduced-motion: reduce)").matches ?? false;
}

export function useAnimatedNumber(target) {
  const [shown, setShown] = useState(0);
  const lastShownRef = useRef(0);

  useEffect(() => {
    if (target === null || target === undefined) return undefined;
    const from = lastShownRef.current;
    const to = Number(target);
    const duration = prefersReducedMotion() ? 0 : DURATION_MS;
    const start = performance.now();
    let frame;

    function step(now) {
      const progress = duration === 0 ? 1 : Math.min(1, (now - start) / duration);
      const eased = 1 - Math.pow(1 - progress, 3);
      const value = progress < 1 ? from + (to - from) * eased : target;
      lastShownRef.current = Number(value);
      setShown(value);
      if (progress < 1) frame = requestAnimationFrame(step);
    }

    frame = requestAnimationFrame(step);
    return () => cancelAnimationFrame(frame);
  }, [target]);

  return shown;
}
