package com.ecocalcer.mixin;

import appeng.api.networking.IGrid;
import appeng.api.networking.crafting.CalculationStrategy;
import appeng.api.networking.crafting.ICraftingPlan;
import appeng.api.networking.crafting.ICraftingSimulationRequester;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.crafting.CraftingCalculation;
import appeng.crafting.CraftingPlan;
import appeng.me.service.CraftingService;
import com.ecocalcer.EcoCalcMod;
import com.ecocalcer.blockentity.FastPlannerBlockEntity;
import com.ecocalcer.planner.CraftingServiceReflect;
import com.ecocalcer.planner.FastCalculationMarker;
import com.ecocalcer.planner.FastPlannerFinder;
import com.ecocalcer.planner.FastPlannerOptions;
import com.ecocalcer.planner.cycle.CycleAnalyzer;
import com.ecocalcer.planner.cycle.CyclePlan;
import com.ecocalcer.planner.cycle.CycleSolver;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

/**
 * Takes over AE2's crafting plan calculation when the planner block in the network has
 * one of its toggles enabled.
 * <ul>
 *     <li><b>Cycle solver</b>: when the requested item is part of a self-replicating
 *     cycle (e.g. smithing template duplication), AE2's planner can never produce a
 *     plan because a pattern's input equals its output. The cycle solver builds a
 *     simulation plan and returns it, so the confirmation screen shows the result
 *     instead of hanging.</li>
 *     <li><b>Fast planner</b>: the newly created calculation is marked as fast so it
 *     skips the per-tick budget (only relevant when GTLCore is not installed; GTLCore
 *     already removes the throttle on its own).</li>
 * </ul>
 * Both hooks target AE2's own class/method names, which are not remapped, hence
 * {@code remap = false}. The private grid field is read through reflection.
 */
@Mixin(CraftingService.class)
public abstract class CraftingServiceMixin {
    /**
     * Cycle-solver interception: if the requested item needs a self-replicating cycle,
     * return our simulation plan instead of letting the vanilla calculation hang.
     */
    @Inject(method = "beginCraftingCalculation", at = @At("HEAD"), cancellable = true, remap = false)
    private void ecocalcer$cyclePlanAtHead(Level level,
                                           ICraftingSimulationRequester simRequester,
                                           AEKey what,
                                           long amount,
                                           CalculationStrategy strategy,
                                           CallbackInfoReturnable<Future<ICraftingPlan>> cir) {
        CraftingService self = (CraftingService) (Object) this;
        IGrid grid = CraftingServiceReflect.grid(self);

        FastPlannerBlockEntity planner = FastPlannerFinder.find(grid);
        if (planner == null) {
            return;
        }

        FastPlannerOptions options = FastPlannerFinder.optionsOf(planner);
        if (!options.cycleSolver()) {
            return;
        }

        try {
            CyclePlan cyclePlan = CycleSolver.solvePlan(grid, what, amount);
            if (cyclePlan == null || !cyclePlan.hasCycle()) {
                return;
            }

            planner.setLastReport(CycleAnalyzer.analyze(grid, what, amount));

            CraftingPlan plan = new CraftingPlan(
                    new GenericStack(what, amount),
                    cyclePlan.bytes(),
                    true, // simulation: shown in the GUI, cannot be dispatched (AE2 CPU cannot self-feed a pattern)
                    false,
                    cyclePlan.usedItems(),
                    cyclePlan.emittedItems(),
                    cyclePlan.missingItems(),
                    cyclePlan.patternTimes());

            cir.setReturnValue(CompletableFuture.completedFuture(plan));
        } catch (Throwable t) {
            // A failed cycle plan must never break crafting plan calculation.
            EcoCalcMod.LOGGER.warn("Cycle plan calculation failed", t);
        }
    }

    /**
     * Fast-planner interception: replaces the {@code new CraftingCalculation(...)} call
     * so the instance can be marked as fast. It still runs through AE2's normal
     * calculation, only without the per-tick pacing.
     */
    @Redirect(method = "beginCraftingCalculation",
            at = @At(value = "NEW", target = "Lappeng/crafting/CraftingCalculation;"),
            remap = false,
            require = 1)
    private CraftingCalculation ecocalcer$markFastCalculation(Level level, IGrid grid,
                                                              ICraftingSimulationRequester simRequester,
                                                              GenericStack output,
                                                              CalculationStrategy strategy) {
        CraftingCalculation calculation = new CraftingCalculation(level, grid, simRequester, output, strategy);

        FastPlannerBlockEntity planner = FastPlannerFinder.find(grid);
        if (planner == null) {
            return calculation;
        }

        FastPlannerOptions options = FastPlannerFinder.optionsOf(planner);

        if (options.cycleSolver()) {
            try {
                planner.setLastReport(CycleAnalyzer.analyze(grid, output.what(), output.amount()));
            } catch (Throwable t) {
                EcoCalcMod.LOGGER.warn("Cycle analysis failed", t);
            }
        }

        if (options.fastPlanner() && calculation instanceof FastCalculationMarker marker) {
            marker.ecocalcer$markFast();
        }
        return calculation;
    }
}
