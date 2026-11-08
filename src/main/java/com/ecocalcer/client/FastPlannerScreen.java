package com.ecocalcer.client;

import com.ecocalcer.menu.FastPlannerMenu;
import com.ecocalcer.net.CycleGraphPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

public class FastPlannerScreen extends Screen implements MenuAccess<FastPlannerMenu> {
    private static final int PANEL_WIDTH = 320;
    private static final int PANEL_HEIGHT = 250;

    private static final int GRAPH_LEFT = 16;
    private static final int GRAPH_TOP = 150;
    private static final int GRAPH_WIDTH = PANEL_WIDTH - 32;
    private static final int GRAPH_HEIGHT = PANEL_HEIGHT - GRAPH_TOP - 12;
    private static final int MAX_DRAWN_NODES = 140;
    private static final int MAX_DRAWN_EDGES = 320;

    private final FastPlannerMenu menu;
    private Button fastPlannerButton;
    private Button cycleSolverButton;

    // Node positions computed during rendering, reused for hover checks.
    private int[] nodeX = new int[0];
    private int[] nodeY = new int[0];
    private int hoveredNode = -1;

    public FastPlannerScreen(FastPlannerMenu menu, net.minecraft.world.entity.player.Inventory inventory,
                             Component title) {
        super(title);
        this.menu = menu;
    }

    @Override
    public FastPlannerMenu getMenu() {
        return this.menu;
    }

    @Override
    protected void init() {
        super.init();
        int left = (this.width - PANEL_WIDTH) / 2;
        int top = (this.height - PANEL_HEIGHT) / 2;

        fastPlannerButton = addRenderableWidget(Button.builder(Component.empty(), b ->
                        press(FastPlannerMenu.BUTTON_TOGGLE_FAST_PLANNER))
                .bounds(left + 24, top + 52, PANEL_WIDTH - 48, 20)
                .build());

        cycleSolverButton = addRenderableWidget(Button.builder(Component.empty(), b ->
                        press(FastPlannerMenu.BUTTON_TOGGLE_CYCLE_SOLVER))
                .bounds(left + 24, top + 76, PANEL_WIDTH - 48, 20)
                .build());
    }

    private void press(int buttonId) {
        if (this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, buttonId);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);

        int left = (this.width - PANEL_WIDTH) / 2;
        int top = (this.height - PANEL_HEIGHT) / 2;

        graphics.fill(left, top, left + PANEL_WIDTH, top + PANEL_HEIGHT, 0xE0141418);
        graphics.fill(left, top, left + PANEL_WIDTH, top + 1, 0xFF3A3A44);
        graphics.fill(left, top + PANEL_HEIGHT - 1, left + PANEL_WIDTH, top + PANEL_HEIGHT, 0xFF3A3A44);
        graphics.fill(left, top, left + 1, top + PANEL_HEIGHT, 0xFF3A3A44);
        graphics.fill(left + PANEL_WIDTH - 1, top, left + PANEL_WIDTH, top + PANEL_HEIGHT, 0xFF3A3A44);

        graphics.drawString(this.font, this.title, left + 24, top + 12, 0xFFE8E8EC, false);

        boolean fast = this.menu.isFastPlannerEnabled();
        boolean cycle = this.menu.isCycleSolverEnabled();

        fastPlannerButton.setMessage(Component.translatable(
                "gui.ecocalcer.fast_planner.toggle.fast", onOff(fast)));
        cycleSolverButton.setMessage(Component.translatable(
                "gui.ecocalcer.fast_planner.toggle.cycle", onOff(cycle)));

        long micros = this.menu.getMenuData(FastPlannerMenu.DATA_LAST_MICROS);
        int cycles = this.menu.getMenuData(FastPlannerMenu.DATA_LAST_CYCLES);

        graphics.drawString(this.font,
                Component.translatable("gui.ecocalcer.fast_planner.stats.time", micros),
                left + 24, top + 104, 0xFFB8B8C0, false);
        graphics.drawString(this.font,
                Component.translatable("gui.ecocalcer.fast_planner.stats.cycles", cycles),
                left + 24, top + 118, 0xFFB8B8C0, false);

        // Divider above the graph area.
        graphics.fill(left + 12, top + GRAPH_TOP - 6, left + PANEL_WIDTH - 12, top + GRAPH_TOP - 5, 0xFF2A2A32);

        CycleGraphPacket graph = ClientCycleData.get(this.menu.getBlockEntity().getBlockPos());
        renderGraph(graphics, graph, left, top, mouseX, mouseY);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderGraph(GuiGraphics graphics, CycleGraphPacket graph, int left, int top, int mouseX, int mouseY) {
        int gx = left + GRAPH_LEFT;
        int gy = top + GRAPH_TOP;
        int gw = GRAPH_WIDTH;
        int gh = GRAPH_HEIGHT;

        hoveredNode = -1;

        if (graph == null || graph.nodes().isEmpty()) {
            graphics.drawString(this.font,
                    Component.translatable("gui.ecocalcer.fast_planner.graph.empty"),
                    gx + 4, gy + gh / 2 - 4, 0xFF70707A, false);
            return;
        }

        int count = Math.min(graph.nodes().size(), MAX_DRAWN_NODES);
        int cols = Math.max(1, (int) Math.ceil(Math.sqrt(count)));
        int rows = Math.max(1, (count + cols - 1) / cols);

        nodeX = new int[count];
        nodeY = new int[count];

        int stepX = cols > 1 ? (gw - 24) / (cols - 1) : 0;
        int stepY = rows > 1 ? (gh - 24) / (rows - 1) : 0;

        for (int i = 0; i < count; i++) {
            int col = i % cols;
            int row = i / cols;
            nodeX[i] = gx + 12 + col * stepX;
            nodeY[i] = gy + 12 + row * stepY;
        }

        // Edges first, so nodes draw on top.
        int drawnEdges = 0;
        for (CycleGraphPacket.EdgeEntry edge : graph.edges()) {
            if (drawnEdges++ >= MAX_DRAWN_EDGES) {
                break;
            }
            int from = edge.from();
            int to = edge.to();
            if (from >= count || to >= count) {
                continue;
            }
            drawLine(graphics, nodeX[from], nodeY[from], nodeX[to], nodeY[to], 0x5535C8E8);
        }

        for (int i = 0; i < count; i++) {
            CycleGraphPacket.NodeEntry node = graph.nodes().get(i);
            int color = node.target() ? 0xFFFFD24D : colorForScc(node.sccId());
            graphics.fill(nodeX[i] - 2, nodeY[i] - 2, nodeX[i] + 2, nodeY[i] + 2, color);

            int dx = mouseX - nodeX[i];
            int dy = mouseY - nodeY[i];
            if (dx * dx + dy * dy <= 25) {
                hoveredNode = i;
            }
        }

        if (hoveredNode >= 0) {
            CycleGraphPacket.NodeEntry node = graph.nodes().get(hoveredNode);
            graphics.renderTooltip(this.font, node.key().getDisplayName(), mouseX, mouseY);
        }
    }

    private static int colorForScc(int sccId) {
        int r = 90 + (sccId * 53) % 140;
        int g = 120 + (sccId * 97) % 120;
        int b = 140 + (sccId * 31) % 110;
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private static void drawLine(GuiGraphics graphics, int x0, int y0, int x1, int y1, int color) {
        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1;
        int sy = y0 < y1 ? 1 : -1;
        int err = dx - dy;
        int steps = 0;
        while (steps++ < 400) {
            graphics.fill(x0, y0, x0 + 1, y0 + 1, color);
            if (x0 == x1 && y0 == y1) {
                break;
            }
            int e2 = 2 * err;
            if (e2 > -dy) {
                err -= dy;
                x0 += sx;
            }
            if (e2 < dx) {
                err += dx;
                y0 += sy;
            }
        }
    }

    private static Component onOff(boolean enabled) {
        return Component.translatable(enabled ? "gui.ecocalcer.on" : "gui.ecocalcer.off");
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
