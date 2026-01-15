/** Copyright (c) 2011-2015, SpaceToad and the BuildCraft Team http://www.mod-buildcraft.com
 *
 * The BuildCraft API is distributed under the terms of the MIT License. Please check the contents of the license, which
 * should be located as "LICENSE.API" in the BuildCraft source code distribution. */
package buildcraft.api.core;

import java.lang.reflect.Method;
import java.util.Locale;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.LoaderState;
import net.minecraftforge.fml.common.ModContainer;

import buildcraft.api.BCModules;

/** Provides a way to quickly enable or disable certain debug conditions via VM arguments or whether the client/server
 * is in a dev environment */
public class BCDebugging {
    public enum DebugStatus {
        NONE,
        ENABLE,
        LOGGING_ONLY,
        ALL
    }

    enum DebugLevel {
        LOG,
        COMPLEX;

        final String name = name().toLowerCase(Locale.ROOT);
        boolean isAllOn;
    }

    private static final DebugStatus DEBUG_STATUS;

    static {
        // Force disable all debugging
        DEBUG_STATUS = DebugStatus.NONE;
        DebugLevel.COMPLEX.isAllOn = false;
        DebugLevel.LOG.isAllOn = false;
    }

    public static boolean shouldDebugComplex(String string) {
        return false;
    }

    public static boolean shouldDebugLog(String string) {
        return false;
    }

    private static boolean shouldDebug(String option, DebugLevel type) {
        return false;
    }

    private static String getProp(String string) {
        return "buildcraft." + string + ".debug";
    }
}
