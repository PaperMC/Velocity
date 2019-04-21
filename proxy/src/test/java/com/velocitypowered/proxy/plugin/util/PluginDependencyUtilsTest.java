package com.velocitypowered.proxy.plugin.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.common.collect.ImmutableList;
import com.velocitypowered.api.plugin.PluginDescription;
import com.velocitypowered.api.plugin.meta.PluginDependency;
import com.velocitypowered.proxy.plugin.loader.VelocityPluginDescription;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.junit.jupiter.api.Test;

class PluginDependencyUtilsTest {

  private static final PluginDescription NO_DEPENDENCY_1_EXAMPLE = testDescription("example");
  private static final PluginDescription NEVER_DEPENDED = testDescription("and-again");
  private static final PluginDescription SOFT_DEPENDENCY_EXISTS = testDescription("soft",
      ImmutableList.of(new PluginDependency("example", "", true)));
  private static final PluginDescription SOFT_DEPENDENCY_DOES_NOT_EXIST = testDescription("fluffy",
      ImmutableList.of(new PluginDependency("i-dont-exist", "", false)));
  private static final PluginDescription MULTI_DEPENDENCY = testDescription("multi-depend",
      ImmutableList.of(
          new PluginDependency("example", "", false)
      )
  );
  private static final PluginDescription TEST_WITH_DUPLICATE_DEPEND = testDescription("dup-depend",
      ImmutableList.of(
          new PluginDependency("multi-depend", "", false)
      )
  );

  private static final PluginDescription CIRCULAR_DEPENDENCY_1 = testDescription("circle",
      ImmutableList.of(new PluginDependency("oval", "", false)));
  private static final PluginDescription CIRCULAR_DEPENDENCY_2 = testDescription("oval",
      ImmutableList.of(new PluginDependency("circle", "", false)));

  private static final ImmutableList<PluginDescription> EXPECTED = ImmutableList.of(
      NEVER_DEPENDED,
      NO_DEPENDENCY_1_EXAMPLE,
      MULTI_DEPENDENCY,
      TEST_WITH_DUPLICATE_DEPEND,
      SOFT_DEPENDENCY_DOES_NOT_EXIST,
      SOFT_DEPENDENCY_EXISTS
  );

  @Test
  void sortCandidates() throws Exception {
    List<PluginDescription> descriptionList = new ArrayList<>();
    descriptionList.add(NO_DEPENDENCY_1_EXAMPLE);
    descriptionList.add(NEVER_DEPENDED);
    descriptionList.add(SOFT_DEPENDENCY_DOES_NOT_EXIST);
    descriptionList.add(SOFT_DEPENDENCY_EXISTS);
    descriptionList.add(MULTI_DEPENDENCY);
    descriptionList.add(TEST_WITH_DUPLICATE_DEPEND);
    descriptionList.sort(Comparator.comparing(PluginDescription::getId));

    assertEquals(EXPECTED, PluginDependencyUtils.sortCandidates(descriptionList));
  }

  @Test
  void sortCandidatesCircularDependency() throws Exception {
    List<PluginDescription> descs = ImmutableList.of(CIRCULAR_DEPENDENCY_1, CIRCULAR_DEPENDENCY_2);
    assertThrows(IllegalStateException.class, () -> PluginDependencyUtils.sortCandidates(descs));
  }

  private static PluginDescription testDescription(String id) {
    return testDescription(id, ImmutableList.of());
  }

  private static PluginDescription testDescription(String id, List<PluginDependency> dependencies) {
    return new VelocityPluginDescription(
        id, "tuxed", "0.1", null, null, ImmutableList.of(),
        dependencies, null
    );
  }
}
