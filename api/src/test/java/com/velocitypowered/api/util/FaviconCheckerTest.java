package com.velocitypowered.api.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.io.Resources;
import java.io.IOException;
import org.junit.jupiter.api.Test;

public class FaviconCheckerTest {

  @Test
  void handlesProperlyFormattedFavicon() throws IOException {
    assertTrue(FaviconChecker.check(readFile("test_icon_dimensions_correct.png")),
        "Correctly formatted favicon is not valid");
  }

  @Test
  void handlesBadDimensionFavicon() throws IOException {
    assertFalse(FaviconChecker.check(readFile("test_icon_dimensions_wrong.png")),
        "Incorrect dimension favicon is valid?");
  }

  @Test
  void handlesBadFormatFavicon() throws IOException {
    assertFalse(FaviconChecker.check(readFile("test_icon_dimensions_wrong_format.jpg")),
        "Incorrect format favicon is valid?");
  }

  private static byte[] readFile(String path) throws IOException {
    return Resources.toByteArray(FaviconCheckerTest.class.getResource(path));
  }
}
