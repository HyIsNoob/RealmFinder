package com.hyisnoob.realmfinder.test;

import com.hyisnoob.realmfinder.core.config.RealmFinderConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RealmFinderConfigTest {
    @Test
    void modpackLimitsStayInsideSafeBounds() {
        var settings = new RealmFinderConfig.Settings();
        settings.maxCapturedBlocks = Integer.MAX_VALUE;
        settings.maxCapturedEntities = -1;
        settings.maxUndoSteps = 100;
        settings.maxCameraZoom = 0;
        settings.validated();
        assertEquals(16384, settings.maxCapturedBlocks);
        assertEquals(0, settings.maxCapturedEntities);
        assertEquals(20, settings.maxUndoSteps);
        assertEquals(1, settings.maxCameraZoom);
    }

    @Test
    void gameplayDefaultsKeepTheExistingFunRules() {
        var settings = new RealmFinderConfig.Settings();
        assertTrue(settings.copyContainerContents);
        assertTrue(settings.copyMobEquipment);
        assertTrue(settings.requireEmptyPhotograph);
        assertTrue(settings.useCameraDurability);
        assertTrue(settings.allowPlacementCarving);
        assertTrue(settings.allowEntityCapture);
    }

    @Test
    void disabledCaptureCostsAndCarvingApplyToSurvivalRules() {
        var settings = new RealmFinderConfig.Settings();
        settings.requireEmptyPhotograph = false;
        settings.useCameraDurability = false;
        settings.allowPlacementCarving = false;
        assertFalse(settings.needsBlank(false));
        assertFalse(settings.damagesCamera(false));
        assertFalse(settings.mayCarve(true, 1));
        assertFalse(settings.mayCarve(true, 0.5f));
    }

    @Test
    void oldConfigGainsNewRulesWithoutChangingOldValues() {
        var migrated = RealmFinderConfig.fromJson("{\"maxUndoSteps\":3,\"allowEntityCapture\":false}");
        assertEquals(3, migrated.maxUndoSteps);
        assertFalse(migrated.allowEntityCapture);
        assertTrue(migrated.copyContainerContents);
        assertTrue(migrated.copyMobEquipment);
    }
}
