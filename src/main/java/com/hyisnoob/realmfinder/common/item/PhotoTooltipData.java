package com.hyisnoob.realmfinder.common.item;

import net.minecraft.world.inventory.tooltip.TooltipComponent;

import java.util.UUID;

public record PhotoTooltipData(UUID snapshotId, int blockCount) implements TooltipComponent {
}
