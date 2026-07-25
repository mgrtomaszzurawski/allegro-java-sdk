/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
 */
package io.github.mgrtomaszzurawski.allegro.sdk.offers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.mgrtomaszzurawski.allegro.client.model.MessageToSellerSettingsRaw;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.MessageToSellerMode;
import io.github.mgrtomaszzurawski.allegro.sdk.domain.offers.model.MessageToSellerSettings;
import org.junit.jupiter.api.Test;

class MessageToSellerSettingsTest {

    private static final String HINT = "Please leave your engraving text.";

    @Test
    void toRaw_whenModeAndHintSet_writesBoth() {
        // when
        MessageToSellerSettingsRaw raw = MessageToSellerSettings.of(MessageToSellerMode.REQUIRED, HINT).toRaw();

        // then
        assertEquals(MessageToSellerSettingsRaw.ModeEnum.REQUIRED, raw.getMode());
        assertEquals(HINT, raw.getHint());
    }

    @Test
    void toRaw_whenOnlyMode_omitsHint() {
        // when
        MessageToSellerSettingsRaw raw = MessageToSellerSettings.of(MessageToSellerMode.HIDDEN).toRaw();

        // then
        assertEquals(MessageToSellerSettingsRaw.ModeEnum.HIDDEN, raw.getMode());
        assertNull(raw.getHint());
    }

    @Test
    void from_whenRawPresent_mapsModeAndHint() {
        // given
        MessageToSellerSettingsRaw raw = new MessageToSellerSettingsRaw()
                .mode(MessageToSellerSettingsRaw.ModeEnum.OPTIONAL).hint(HINT);

        // when
        MessageToSellerSettings settings = MessageToSellerSettings.from(raw);

        // then
        assertEquals(MessageToSellerMode.OPTIONAL, settings.mode());
        assertEquals(HINT, settings.hint());
    }

    @Test
    void from_whenRawNull_returnsNull() {
        // then
        assertNull(MessageToSellerSettings.from(null));
    }

    @Test
    void mode_fromRaw_mapsEveryKnownValueTheSentinelAndNull() {
        // then — each known value maps 1:1, the open-api sentinel degrades to UNKNOWN, null stays null
        assertEquals(MessageToSellerMode.REQUIRED,
                MessageToSellerMode.from(MessageToSellerSettingsRaw.ModeEnum.REQUIRED));
        assertEquals(MessageToSellerMode.OPTIONAL,
                MessageToSellerMode.from(MessageToSellerSettingsRaw.ModeEnum.OPTIONAL));
        assertEquals(MessageToSellerMode.HIDDEN,
                MessageToSellerMode.from(MessageToSellerSettingsRaw.ModeEnum.HIDDEN));
        assertEquals(MessageToSellerMode.UNKNOWN,
                MessageToSellerMode.from(MessageToSellerSettingsRaw.ModeEnum.UNKNOWN_DEFAULT_OPEN_API));
        assertNull(MessageToSellerMode.from(null));
    }

    @Test
    void mode_toRaw_mapsEveryRequestableValue() {
        // then — each client-settable mode maps 1:1 to the generated enum
        assertEquals(MessageToSellerSettingsRaw.ModeEnum.REQUIRED, MessageToSellerMode.REQUIRED.toRaw());
        assertEquals(MessageToSellerSettingsRaw.ModeEnum.OPTIONAL, MessageToSellerMode.OPTIONAL.toRaw());
        assertEquals(MessageToSellerSettingsRaw.ModeEnum.HIDDEN, MessageToSellerMode.HIDDEN.toRaw());
    }

    @Test
    void mode_toRaw_whenUnknown_throwsIllegalArgument() {
        // UNKNOWN is not a mode a client can request
        assertThrows(IllegalArgumentException.class, MessageToSellerMode.UNKNOWN::toRaw);
    }
}
