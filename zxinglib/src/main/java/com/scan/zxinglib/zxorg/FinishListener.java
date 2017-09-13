package com.scan.zxinglib.zxorg;

import android.app.Activity;
import android.content.DialogInterface;

/**
 * Created by admin on 2017/9/12.
 */
public final class FinishListener implements DialogInterface.OnClickListener, DialogInterface.OnCancelListener {

    private final Activity activityToFinish;

    public FinishListener(Activity activityToFinish) {
        this.activityToFinish = activityToFinish;
    }

    @Override
    public void onCancel(DialogInterface dialogInterface) {
        run();
    }

    @Override
    public void onClick(DialogInterface dialogInterface, int i) {
        run();
    }

    private void run() {
        activityToFinish.finish();
    }

}
