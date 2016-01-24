package de.dknapps.mindbell.test.accessors;

import android.test.AndroidTestCase;
import de.dknapps.mindbell.accessors.AndroidContextAccessor;
import de.dknapps.mindbell.accessors.ContextAccessor;

public class AndroidContextAccessorTest extends AndroidTestCase {

    private ContextAccessor createContextAccessor() {
        return AndroidContextAccessor.get(getContext());
    }

    public void testBellVolume() {
        // setup
        ContextAccessor ca = createContextAccessor();
        // exercise
        ca.startBellSound(null);
        // verify
        assertEquals(ca.getAlarmMaxVolume(), ca.getAlarmVolume());
    }

    public void testFinish() {
        // setup
        ContextAccessor ca = createContextAccessor();
        ca.setAlarmVolume(ca.getAlarmMaxVolume() / 2);
        int alarmVolume = ca.getAlarmVolume();
        // exercise
        ca.startBellSound(null);
        ca.finishBellSound();
        // verify
        assertFalse(ca.isBellSoundPlaying());
        assertEquals(alarmVolume, ca.getAlarmVolume());
    }

    public void testOriginalVolume() {
        // setup
        ContextAccessor ca = createContextAccessor();
        int originalVolume = ca.getAlarmVolume();
        // exercise
        ca.startBellSound(null);
        ca.finishBellSound();
        // verify
        assertEquals(originalVolume, ca.getAlarmVolume());
    }

    public void testPlay() {
        // setup
        ContextAccessor ca = createContextAccessor();
        // exercise
        ca.startBellSound(null);
        // verify
        assertTrue(ca.isBellSoundPlaying());
    }
}
