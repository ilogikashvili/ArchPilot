import { create } from "zustand";
import type { SimResult } from "./engine";

interface SimulationState {
  open: boolean;
  result: SimResult | null;
  /** Index of the packet currently travelling. */
  current: number;
  /** 0..1 progress of the current packet along its connection. */
  progress: number;
  playing: boolean;
  speed: number;
  set: (patch: Partial<Omit<SimulationState, "set">>) => void;
}

export const useSimulation = create<SimulationState>((set) => ({
  open: false, result: null, current: 0, progress: 0, playing: false, speed: 1,
  set: (patch) => set(patch),
}));
