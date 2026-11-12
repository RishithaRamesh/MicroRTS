package courseproject;

import static org.junit.Assert.assertEquals;

import courseproject.factoryfeud.FactoryFeudVisualConfig;
import org.junit.Test;

public class FactoryFeudVisualConfigTest {
    @Test
    public void formatsOpenGeneratedMapName() {
        assertDisplayName(
                "factory-feud-pcg-v1-open-022-low-seed-584021-16x12.xml",
                16, 12, "OPEN-022 | Low | 16\u00d712");
    }

    @Test
    public void formatsObstacleFieldGeneratedMapName() {
        assertDisplayName(
                "factory-feud-pcg-v1-obstacle-field-015-medium-seed-1584014-16x12.xml",
                16, 12, "OBSTACLE_FIELD-015 | Medium | 16\u00d712");
    }

    @Test
    public void formatsChokepointGeneratedMapName() {
        assertDisplayName(
                "factory-feud-pcg-v1-chokepoint-001-sparse-seed-2584000-16x12.xml",
                16, 12, "CHOKEPOINT-001 | Sparse | 16\u00d712");
    }

    @Test
    public void retainsHandcraftedMapNameAndDimensions() {
        assertDisplayName(
                "factory-feud-narrow-passage-12x12.xml",
                12, 12, "factory-feud-narrow-passage-12x12.xml (12x12)");
    }

    @Test
    public void fallsBackGracefullyForUnexpectedGeneratedName() {
        assertDisplayName(
                "factory-feud-pcg-v1-maze-999-odd-seed-1-16x12.xml",
                16, 12,
                "factory-feud-pcg-v1-maze-999-odd-seed-1-16x12.xml (16x12)");
    }

    private static void assertDisplayName(
            String fileName, int mapWidth, int mapHeight, String expected) {
        FactoryFeudVisualConfig config = FactoryFeudVisualConfig.parse(new String[]{
            "500", "0", "maps/factory-feud/generated/" + fileName
        });
        assertEquals(expected, config.getMapDisplayName(mapWidth, mapHeight));
    }
}
