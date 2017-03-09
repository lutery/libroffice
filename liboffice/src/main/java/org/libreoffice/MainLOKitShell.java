package org.libreoffice;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.PointF;
import android.graphics.RectF;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.KeyEvent;

import org.libreoffice.canvas.SelectionHandle;
import org.mozilla.gecko.gfx.ComposedTileLayer;
import org.mozilla.gecko.gfx.LayerView;

/**
 * Created by 辉 on 2016/12/28.
 */

public class MainLOKitShell {
    private static final String LOGTAG = LOKitShell.class.getSimpleName();

    public static float getDpi() {
        DisplayMetrics metrics = MainOffice.mContext.getResources().getDisplayMetrics();
        return metrics.density * 160;
    }

    // Get a Handler for the main java thread
//    public static Handler getMainHandler() {
//        return LibreOfficeMainActivity.mAppContext.mMainHandler;
//    }

//    public static void showProgressSpinner() {
//        getMainHandler().post(new Runnable() {
//            @Override
//            public void run() {
//                LibreOfficeMainActivity.mAppContext.showProgressSpinner();
//            }
//        });
//    }

//    public static void hideProgressSpinner() {
//        getMainHandler().post(new Runnable() {
//            @Override
//            public void run() {
//                LibreOfficeMainActivity.mAppContext.hideProgressSpinner();
//            }
//        });
//    }

//    public static ToolbarController getToolbarController() {
//        return LibreOfficeMainActivity.mAppContext.getToolbarController();
//    }

//    public static FormattingController getFormattingController() {
//        return LibreOfficeMainActivity.mAppContext.getFormattingController();
//    }
//
//    public static FontController getFontController() {
//        return LibreOfficeMainActivity.mAppContext.getFontController();
//    }

    public static int getMemoryClass(Context context) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        return activityManager.getMemoryClass() * 1024 * 1024;
    }

//    public static DisplayMetrics getDisplayMetrics() {
//        if (MainOffice.mAppContext == null) {
//            return null;
//        }
//        DisplayMetrics metrics = new DisplayMetrics();
//        MainOffice.mContext.getWindowManager().getDefaultDisplay().getMetrics(metrics);
//        return metrics;
//    }

//    public static boolean isEditingEnabled() {
//        return LibreOfficeMainActivity.isExperimentalMode();
//    }

//    public static LayerView getLayerView() {
//        return LibreOfficeMainActivity.getLayerClient().getView();
//    }

    // EVENTS

    /**
     * Make sure LOKitThread is running and send event to it.
     */
    public static void sendEvent(LOEvent event) {
        if (MainOffice.mAppContext != null && MainOffice.mAppContext.getLOKitThread() != null) {
            MainOffice.mAppContext.getLOKitThread().queueEvent(event);
        }
    }

    public static void sendThumbnailEvent(ThumbnailCreator.ThumbnailCreationTask task) {
        MainLOKitShell.sendEvent(new LOEvent(LOEvent.THUMBNAIL, task));
    }

    /**
     * Send touch event to LOKitThread.
     */
    public static void sendTouchEvent(String touchType, PointF documentTouchCoordinate) {
        MainLOKitShell.sendEvent(new LOEvent(LOEvent.TOUCH, touchType, documentTouchCoordinate));
    }

    /**
     * Send key event to LOKitThread.
     */
    public static void sendKeyEvent(KeyEvent event) {
        MainLOKitShell.sendEvent(new LOEvent(LOEvent.KEY_EVENT, event));
    }

    public static void sendSizeChangedEvent(int width, int height) {
        MainLOKitShell.sendEvent(new LOEvent(LOEvent.SIZE_CHANGED));
    }

    public static void sendSwipeRightEvent() {
        MainLOKitShell.sendEvent(new LOEvent(LOEvent.SWIPE_RIGHT));
    }

    public static void sendSwipeLeftEvent() {
        MainLOKitShell.sendEvent(new LOEvent(LOEvent.SWIPE_LEFT));
    }

    public static void sendChangePartEvent(int part) {
        MainLOKitShell.sendEvent(new LOEvent(LOEvent.CHANGE_PART, part));
    }

    public static void sendLoadEvent(String inputFile) {
        MainLOKitShell.sendEvent(new LOEvent(LOEvent.LOAD, inputFile));
    }

    public static void sendLoadEvent(String inputFile, int renderWidth, int renderHeight){
        MainLOKitShell.sendEvent(new LOEvent(LOEvent.LOAD_SIZE, inputFile, renderWidth, renderHeight));
    }

    public static void sendCloseEvent() {
        MainLOKitShell.sendEvent(new LOEvent(LOEvent.CLOSE));
    }

    /**
     * Send tile reevaluation to LOKitThread.
     */
    public static void sendTileReevaluationRequest(ComposedTileLayer composedTileLayer) {
        MainLOKitShell.sendEvent(new LOEvent(LOEvent.TILE_REEVALUATION_REQUEST, composedTileLayer));
    }

    /**
     * Send tile invalidation to LOKitThread.
     */
    public static void sendTileInvalidationRequest(RectF rect) {
        MainLOKitShell.sendEvent(new LOEvent(LOEvent.TILE_INVALIDATION, rect));
    }

    /**
     * Send change handle position event to LOKitThread.
     */
    public static void sendChangeHandlePositionEvent(SelectionHandle.HandleType handleType, PointF documentCoordinate) {
        MainLOKitShell.sendEvent(new LOEvent(LOEvent.CHANGE_HANDLE_POSITION, handleType, documentCoordinate));
    }

    public static void sendNavigationClickEvent() {
        MainLOKitShell.sendEvent(new LOEvent(LOEvent.NAVIGATION_CLICK));
    }

    /**
     * Move the viewport to the desired point (top-left), and change the zoom level.
     * Ensure this runs on the UI thread.
     */
    public static void moveViewportTo(final PointF position, final Float zoom) {
//        getLayerView().getLayerClient().post(new Runnable() {
//            @Override
//            public void run() {
//                getLayerView().getLayerClient().moveTo(position, zoom);
//            }
//        });
    }
}
