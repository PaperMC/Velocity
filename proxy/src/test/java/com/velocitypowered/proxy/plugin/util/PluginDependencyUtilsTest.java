package com.velocitypowered.proxy.plugin.util;

import com.google.common.collect.ImmutableList;
import com.velocitypowered.api.plugin.PluginDescription;
import com.velocitypowered.api.plugin.meta.PluginDependency;
import com.velocitypowered.proxy.plugin.loader.VelocityPluginDescription;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PluginDependencyUtilsTest {
    private static final PluginDescription NO_DEPENDENCY_1_EXAMPLE = new VelocityPluginDescription(
            "example", "tuxed", "0.1", null, null, ImmutableList.of("example2"),
            ImmutableList.of(), null
    );
    private static final PluginDescription NO_DEPENDENCY_2_EXAMPLE = new VelocityPluginDescription(
            "example2", "tuxed", "0.1", null, null, ImmutableList.of(), ImmutableList.of(), null
    );
    private static final PluginDescription NEVER_DEPENDED = new VelocityPluginDescription(
            "and-again", "tuxed", "0.1", null, null, ImmutableList.of(), ImmutableList.of(), null
    );
    private static final PluginDescription SOFT_DEPENDENCY_EXISTS = new VelocityPluginDescription(
            "soft", "tuxed", "0.1", null, null, ImmutableList.of(),
            ImmutableList.of(new PluginDependency("example", "", true)), null
    );
    private static final PluginDescription SOFT_DEPENDENCY_DOES_NOT_EXIST = new VelocityPluginDescription(
            "fluffy", "tuxed", "0.1", null, null, ImmutableList.of(),
            ImmutableList.of(new PluginDependency("i-dont-exist", "", false)), null
    );
    private static final PluginDescription MULTI_DEPENDENCY = new VelocityPluginDescription(
            "multi-depend", "tuxed", "0.1", null, null, ImmutableList.of(),
            ImmutableList.of(
                    new PluginDependency("example", "", false),
                    new PluginDependency("example2", "", false)
            ), null
    );

    private static final PluginDescription CIRCULAR_DEPENDENCY_1 = new VelocityPluginDescription(
            "circle", "tuxed", "0.1", null, null, ImmutableList.of(),
            ImmutableList.of(
                    new PluginDependency("oval", "", false)
            ), null
    );
    private static final PluginDescription CIRCULAR_DEPENDENCY_2 = new VelocityPluginDescription(
            "oval", "tuxed", "0.1", null, null, ImmutableList.of(),
            ImmutableList.of(
                    new PluginDependency("circle", "", false)
                    ), null
    );

    // Note: Kahn's algorithm is non-unique in its return result, although the topological sort will have the correct
    // order.
    private static final List<PluginDescription> EXPECTED = ImmutableList.of(
            NO_DEPENDENCY_1_EXAMPLE,
            NO_DEPENDENCY_2_EXAMPLE,
            NEVER_DEPENDED,
            SOFT_DEPENDENCY_DOES_NOT_EXIST,
            SOFT_DEPENDENCY_EXISTS,
            MULTI_DEPENDENCY
    );

    @Test
    void sortCandidates() throws Exception {
        List<PluginDescription> descriptionList = new ArrayList<>();
        descriptionList.add(NO_DEPENDENCY_1_EXAMPLE);
        descriptionList.add(NO_DEPENDENCY_2_EXAMPLE);
        descriptionList.add(NEVER_DEPENDED);
        descriptionList.add(SOFT_DEPENDENCY_DOES_NOT_EXIST);
        descriptionList.add(SOFT_DEPENDENCY_EXISTS);
        descriptionList.add(MULTI_DEPENDENCY);

        assertEquals(EXPECTED, PluginDependencyUtils.sortCandidates(descriptionList));
    }

    @Test
    void sortCandidatesCircularDependency() throws Exception {
        List<PluginDescription> descs = ImmutableList.of(CIRCULAR_DEPENDENCY_1, CIRCULAR_DEPENDENCY_2);
        assertThrows(IllegalStateException.class, () -> PluginDependencyUtils.sortCandidates(descs));
    }
}
