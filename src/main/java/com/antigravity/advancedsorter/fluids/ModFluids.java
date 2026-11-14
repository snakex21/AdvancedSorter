package com.antigravity.advancedsorter.fluids;

import com.antigravity.advancedsorter.AdvancedSorterMod;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;

public class ModFluids {

    public static Fluid LIQUID_XP;

    public static void registerFluids() {
        // Define Liquid XP using vanilla water textures but we will color it green in GUI
        ResourceLocation still = new ResourceLocation("minecraft", "blocks/water_still");
        ResourceLocation flow = new ResourceLocation("minecraft", "blocks/water_flow");

        LIQUID_XP = new Fluid("liquid_xp", still, flow)
                .setColor(0xFF55FF55) // Bright Lime Green
                .setLuminosity(10)    // Glowing
                .setViscosity(1000);

        FluidRegistry.registerFluid(LIQUID_XP);

        // Add bucket (optional, handled by Universal Bucket mostly)
        FluidRegistry.addBucketForFluid(LIQUID_XP);
    }
}
