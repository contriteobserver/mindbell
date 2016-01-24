/**
 *
 */
package de.dknapps.mindbell.test;

import android.app.Activity;
import android.test.ActivityInstrumentationTestCase2;
import android.view.View;
import de.dknapps.mindbell.MindBell;

/**
 * @author marc
 *
 */
public class MindBellTest extends ActivityInstrumentationTestCase2<MindBell> {

    private Activity mActivity;
    private View mView;

    public MindBellTest() {
        super("de.dknapps.mindbell", MindBell.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mActivity = getActivity();
        mView = mActivity.findViewById(de.dknapps.mindbell.R.id.bell);

    }

    public void testPreconditions() {
        assertNotNull(mView);
    }
}
