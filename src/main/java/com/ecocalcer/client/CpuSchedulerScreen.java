package com.ecocalcer.client;

import com.ecocalcer.menu.CpuSchedulerMenu;
import com.ecocalcer.net.CpuListPacket;
import com.ecocalcer.net.CpuTogglePacket;
import com.ecocalcer.net.ModNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

public class CpuSchedulerScreen extends Screen implements MenuAccess<CpuSchedulerMenu> {
    private static final int PANEL_WIDTH = 340;
    private static final int PANEL_HEIGHT = 220;
    private static final int ROW_HEIGHT = 22;
    private static final int MAX_ROWS = 8;

    private final CpuSchedulerMenu menu;

    public CpuSchedulerScreen(CpuSchedulerMenu menu, net.minecraft.world.entity.player.Inventory inventory,
                              Component title) {
        super(title);
        this.menu = menu;
    }

    @Override
    public CpuSchedulerMenu getMenu() {
        return this.menu;
    }

    private int panelLeft() {
        return (this.width - PANEL_WIDTH) / 2;
    }

    private int panelTop() {
        return (this.height - PANEL_HEIGHT) / 2;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);

        int left = panelLeft();
        int top = panelTop();

        graphics.fill(left, top, left + PANEL_WIDTH, top + PANEL_HEIGHT, 0xE0141418);
        graphics.fill(left, top, left + PANEL_WIDTH, top + 1, 0xFF3A3A44);
        graphics.fill(left, top + PANEL_HEIGHT - 1, left + PANEL_WIDTH, top + PANEL_HEIGHT, 0xFF3A3A44);
        graphics.fill(left, top, left + 1, top + PANEL_HEIGHT, 0xFF3A3A44);
        graphics.fill(left + PANEL_WIDTH - 1, top, left + PANEL_WIDTH, top + PANEL_HEIGHT, 0xFF3A3A44);

        graphics.drawString(this.font, this.title, left + 12, top + 10, 0xFFE8E8EC, false);

        List<CpuListPacket.Entry> cpus = ClientCpuData.get(this.menu.getBlockEntity().getBlockPos());
        if (cpus.isEmpty()) {
            graphics.drawString(this.font,
                    Component.translatable("gui.ecocalcer.cpu_scheduler.empty"),
                    left + 12, top + 40, 0xFF70707A, false);
        }

        for (int i = 0; i < Math.min(cpus.size(), MAX_ROWS); i++) {
            CpuListPacket.Entry entry = cpus.get(i);
            int y = top + 32 + i * ROW_HEIGHT;

            graphics.fill(left + 8, y, left + PANEL_WIDTH - 8, y + ROW_HEIGHT - 2, 0x88202024);

            String label = entry.name();
            graphics.drawString(this.font, label, left + 14, y + 4, 0xFFD8D8E0, false);

            String status = entry.busy()
                    ? Component.translatable("gui.ecocalcer.cpu_scheduler.busy").getString()
                    : Component.translatable("gui.ecocalcer.cpu_scheduler.idle").getString();
            graphics.drawString(this.font, status, left + 130, y + 4,
                    entry.busy() ? 0xFFFF9F43 : 0xFF55D67A, false);

            graphics.drawString(this.font, String.valueOf(entry.coProcessors()),
                    left + 180, y + 4, 0xFFB8B8C0, false);

            boolean hovered = mouseX >= left + PANEL_WIDTH - 108 && mouseX <= left + PANEL_WIDTH - 14
                    && mouseY >= y && mouseY <= y + ROW_HEIGHT - 2;
            int color = entry.managed() ? 0xFFE05A5A : 0xFF3A8F5A;
            graphics.fill(left + PANEL_WIDTH - 108, y + 2, left + PANEL_WIDTH - 14, y + ROW_HEIGHT - 4,
                    hovered ? (color | 0xFF000000) : (color | 0x99000000));
            Component button = Component.translatable(entry.managed()
                    ? "gui.ecocalcer.cpu_scheduler.remove"
                    : "gui.ecocalcer.cpu_scheduler.add");
            int textWidth = this.font.width(button);
            graphics.drawString(this.font, button,
                    left + PANEL_WIDTH - 61 - textWidth / 2, y + 4, 0xFFFFFFFF, false);
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int left = panelLeft();
        int top = panelTop();
        List<CpuListPacket.Entry> cpus = ClientCpuData.get(this.menu.getBlockEntity().getBlockPos());

        for (int i = 0; i < Math.min(cpus.size(), MAX_ROWS); i++) {
            CpuListPacket.Entry entry = cpus.get(i);
            int y = top + 32 + i * ROW_HEIGHT;
            if (mouseX >= left + PANEL_WIDTH - 108 && mouseX <= left + PANEL_WIDTH - 14
                    && mouseY >= y && mouseY <= y + ROW_HEIGHT - 2) {
                ModNetwork.sendToServer(new CpuTogglePacket(
                        this.menu.getBlockEntity().getBlockPos(), entry.boundsMin()));
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
