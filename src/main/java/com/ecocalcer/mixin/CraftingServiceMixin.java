package com.ecocalcer.mixin;

import appeng.api.networking.IGrid;
import appeng.api.networking.crafting.CalculationStrategy;
import appeng.api.networking.crafting.ICraftingSimulationRequester;
import appeng.api.stacks.GenericStack;
import appeng.crafting.CraftingCalculation;
import appeng.me.service.CraftingService;
import com.ecocalcer.EcoCalcMod;
import com.ecocalcer.blockentity.FastPlannerBlockEntity;
import com.ecocalcer.planner.CraftingServiceReflect;
import com.ecocalcer.planner.FastCalculationMarker;
import com.ecocalcer.planner.FastPlannerFinder;
import com.ecocalcer.planner.FastPlannerOptions;
import com.ecocalcer.planner.cycle.CycleAnalyzer;
import com.ecocalcer.planner.cycle.CycleReport;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Marks AE2's crafting calculation as "fast" when the planner block in the network has the
 * "fast planner" toggle enabled, and runs the cycle analysis before the calculation starts.
 * <p>
 * The generated plan has exactly the same contents as the vanilla one: only the pacing
 * (the 5 microsecond per-tick budget) is removed, so this is a pure performance change.
 * <p>
 * The mixin targets AE2's own class and method names, which are not remapped, hence
 * {@code remap = false}.
 */
@Mixin(CraftingService.class)
public abstract class CraftingServiceMixin {
    /**
     * Replaces the {@code new CraftingCalculation(level, grid, simRequester, output, strategy)}
     * call inside {@code beginCraftingCalculation} so the freshly created instance can be
     * marked as fast. The instance is otherwise identical to the vanilla one.
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
                CycleReport report = CycleAnalyzer.analyze(grid, output.what(), output.amount());
                planner.setLastReport(report);
            } catch (Throwable t) {
                // A failed analysis must never break crafting plan calculation.
                EcoCalcMod.LOGGER.warn("Cycle analysis failed", t);
            }
        }

        if (options.fastPlanner() && calculation instanceof FastCalculationMarker marker) {
            marker.ecocalcer$markFast();
        }
        return calculation;
    }
}
