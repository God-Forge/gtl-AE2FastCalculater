package com.ecocalcer.mixin;

import appeng.crafting.CraftingCalculation;
import com.ecocalcer.planner.FastCalculationMarker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Marks fast calculations and lets them skip the per-tick pause.
 * <p>
 * IMPORTANT: we intentionally do NOT touch {@code run()}. GTLCore (shipped in the GTL
 * packs) {@code @Overwrite}s {@code run()} and {@code simulateFor()}. Redirecting into an
 * overwritten method makes the whole mixin fail with a FATAL error, which in turn breaks
 * {@code CraftingCalculation} class loading — every crafting plan then hangs forever.
 * <p>
 * With GTLCore installed, GTLCore's calculation mode already removes the per-tick
 * throttle (its non-adaptive {@code handlePausing} returns immediately and its
 * {@code simulateFor} does not wait), so this hook is only a fallback for networks
 * without GTLCore.
 */
@Mixin(CraftingCalculation.class)
public abstract class CraftingCalculationMixin implements FastCalculationMarker {
    @Unique
    private boolean ecocalcer$fast;

    @Override
    public void ecocalcer$markFast() {
        this.ecocalcer$fast = true;
    }

    @Override
    public boolean ecocalcer$isFast() {
        return this.ecocalcer$fast;
    }

    @Inject(method = "handlePausing", at = @At("HEAD"), cancellable = true, remap = false)
    private void ecocalcer$skipPausing(CallbackInfo ci) {
        if (!this.ecocalcer$fast) {
            return;
        }
        // Do not pause for the TickHandler budget. Keep interrupt responsiveness so the
        // calculation can still be aborted.
        if (Thread.interrupted()) {
            throw new RuntimeException("Fast crafting calculation interrupted");
        }
        ci.cancel();
    }
}
