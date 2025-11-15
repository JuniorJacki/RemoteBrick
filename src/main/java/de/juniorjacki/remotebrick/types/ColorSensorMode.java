/*
 * Copyright (c) 2025 JuniorJacki
 * All Rights Reserved
 */

package de.juniorjacki.remotebrick.types;

import de.juniorjacki.remotebrick.utils.JsonBuilder;

public enum ColorSensorMode {
    TUPLES(JsonBuilder.object().addArray("mode",1,0,0,0,5,0,5,1,5,2)),
    RAW(JsonBuilder.object().add("mode",2));

    private final JsonBuilder modeJson;

    ColorSensorMode(JsonBuilder json) {
        this.modeJson = json;
    }
    public JsonBuilder getModeJson() {
        return modeJson;
    }
}
