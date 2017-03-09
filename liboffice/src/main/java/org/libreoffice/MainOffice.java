package org.libreoffice;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.os.Handler;
import android.preference.PreferenceManager;
//import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import org.libreoffice.overlay.DocumentOverlay;
import org.mozilla.gecko.ZoomConstraints;
import org.mozilla.gecko.gfx.GeckoLayerClient;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by 辉 on 2016/12/23.
 * 主解析Office类
 */
public class MainOffice {
    public final static String PARSE_START = "cn.com.itep.printer.startiprint.parsestart";
    public final static String PARSE_END = "cn.com.itep.printer.startiprint.parseend";
    public final static String PARSE_END_RESULT = "cn.com.itep.printer.startprint.endresult";
    public final static String PARSE_ERROR = "cn.com.itep.printer.startiprint.parseerror";

    private static MainOffice sMainOffice = null;
    public static Context mContext;
    public static MainOffice mAppContext;

    private MainOffice(Context context){
        mContext = context.getApplicationContext() == null ? context : context.getApplicationContext();
    }

    public static synchronized MainOffice Instance(Context context){
        if (sMainOffice == null){
            sMainOffice = new MainOffice(context);
            mAppContext = sMainOffice;
        }

        return sMainOffice;
    }

    private static final String LOGTAG = "LibreOfficeMainActivity";
    private static final String DEFAULT_DOC_PATH = "/assets/example.odt";
    private static final String ENABLE_EXPERIMENTAL_PREFS_KEY = "ENABLE_EXPERIMENTAL";
    private static final String ASSETS_EXTRACTED_PREFS_KEY = "ASSETS_EXTRACTED";

    private static GeckoLayerClient mLayerClient;
    private static MainLOKitThread sLOKitThread;

    private static boolean mIsExperimentalMode;

    private int providerId;
    private URI documentUri;

    public Handler mMainHandler;

//    private DrawerLayout mDrawerLayout;
//    private LOAbout mAbout;

//    private ListView mDrawerList;
//    private List<DocumentPartView> mDocumentPartView = new ArrayList<DocumentPartView>();
//    private DocumentPartViewListAdapter mDocumentPartViewListAdapter;
    private File mInputFile;
//    private DocumentOverlay mDocumentOverlay;
//    private File mTempFile = null;

//    private FormattingController mFormattingController;
//    private ToolbarController mToolbarController;
    private FontController mFontController;
//    private SearchController mSearchController;

    public static GeckoLayerClient getLayerClient() {
        return mLayerClient;
    }

//    public static boolean isExperimentalMode() {
//        return mIsExperimentalMode;
//    }

//    public boolean usesTemporaryFile() {
//        return mTempFile != null;
//    }

//    private boolean isKeyboardOpen = false;

    public static void sendBroadCast(String ACTION){
        sendBroadCast(ACTION, null);
    }

    public static void sendBroadCast(String ACTION, ArrayList<String> arrList){
        Intent intent = new Intent();
        intent.setAction(ACTION);
        intent.putStringArrayListExtra(PARSE_END_RESULT, arrList);
        mContext.sendBroadcast(intent);
    }

    public void Load(String filePath) {
        Log.w(LOGTAG, "onCreate..");

        SharedPreferences sPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        mIsExperimentalMode = sPrefs.getBoolean(ENABLE_EXPERIMENTAL_PREFS_KEY, false);

        if (sPrefs.getInt(ASSETS_EXTRACTED_PREFS_KEY, 0) != BuildConfig.VERSION_CODE) {
            if(copyFromAssets(mContext.getAssets(), "unpack", mContext.getApplicationInfo().dataDir)) {
                sPrefs.edit().putInt(ASSETS_EXTRACTED_PREFS_KEY, BuildConfig.VERSION_CODE).apply();
            }
        }
        mMainHandler = new Handler();

        mFontController = new FontController(mContext);
//        mSearchController = new SearchController(mContext);

        mInputFile = new File(filePath);
//        if (getIntent().getData() != null) {
//            if (getIntent().getData().getScheme().equals(ContentResolver.SCHEME_CONTENT)) {
//                if (copyFileToTemp() && mTempFile != null) {
//                    mInputFile = mTempFile;
//                    Log.d(LOGTAG, "SCHEME_CONTENT: getPath(): " + getIntent().getData().getPath());
//                } else {
//                    // TODO: can't open the file
//                    Log.e(LOGTAG, "couldn't create temporary file from " + getIntent().getData());
//                }
//            } else if (getIntent().getData().getScheme().equals(ContentResolver.SCHEME_FILE)) {
//                mInputFile = new File(getIntent().getData().getPath());
//                Log.d(LOGTAG, "SCHEME_FILE: getPath(): " + getIntent().getData().getPath());
//
//                // Gather data to rebuild IFile object later
//                providerId = getIntent().getIntExtra(
//                        "org.libreoffice.document_provider_id", 0);
//                documentUri = (URI) getIntent().getSerializableExtra(
//                        "org.libreoffice.document_uri");
//            }
//        } else {
//            mInputFile = new File(DEFAULT_DOC_PATH);
//        }

//        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

//        if (mDocumentPartViewListAdapter == null) {
//            mDrawerList = (ListView) findViewById(R.id.left_drawer);
//
//            mDocumentPartViewListAdapter = new DocumentPartViewListAdapter(this, R.layout.document_part_list_layout, mDocumentPartView);
//            mDrawerList.setAdapter(mDocumentPartViewListAdapter);
//            mDrawerList.setOnItemClickListener(new LibreOfficeMainActivity.DocumentPartClickListener());
//        }

        if (sLOKitThread == null) {
            sLOKitThread = new MainLOKitThread();
            sLOKitThread.start();
        } else {
            sLOKitThread.clearQueue();
        }

        mLayerClient = new GeckoLayerClient(mContext);
        mLayerClient.setZoomConstraints(new ZoomConstraints(true));
//        LayerView layerView = (LayerView) findViewById(R.id.layer_view);
//        mLayerClient.setView(layerView);
//        layerView.setInputConnectionHandler(new LOKitInputConnectionHandler());
//        mLayerClient.notifyReady();

        // create TextCursorLayer
//        mDocumentOverlay = new DocumentOverlay(mAppContext, layerView);

//        mToolbarController.setupToolbars();
    }

//    public RectF getCurrentCursorPosition() {
//        return mDocumentOverlay.getCurrentCursorPosition();
//    }

//    private boolean copyFileToTemp() {
//        ContentResolver contentResolver = mContext.getContentResolver();
//        FileChannel inputChannel = null;
//        FileChannel outputChannel = null;
//        // CSV files need a .csv suffix to be opened in Calc.
//        String suffix = null;
////        String intentType = getIntent().getType();
//        // K-9 mail uses the first, GMail uses the second variant.
////        if ("text/comma-separated-values".equals(intentType) || "text/csv".equals(intentType))
////            suffix = ".csv";
//
//        try {
//            try {
//                AssetFileDescriptor assetFD = contentResolver.openAssetFileDescriptor(getIntent().getData(), "r");
//                if (assetFD == null) {
//                    Log.e(LOGTAG, "couldn't create assetfiledescriptor from " + getIntent().getDataString());
//                    return false;
//                }
//                inputChannel = assetFD.createInputStream().getChannel();
//                mTempFile = File.createTempFile("LibreOffice", suffix, this.getCacheDir());
//
//                outputChannel = new FileOutputStream(mTempFile).getChannel();
//                long bytesTransferred = 0;
//                // might not  copy all at once, so make sure everything gets copied....
//                while (bytesTransferred < inputChannel.size()) {
//                    bytesTransferred += outputChannel.transferFrom(inputChannel, bytesTransferred, inputChannel.size());
//                }
//                Log.e(LOGTAG, "Success copying " + bytesTransferred + " bytes");
//                return true;
//            } finally {
//                if (inputChannel != null) inputChannel.close();
//                if (outputChannel != null) outputChannel.close();
//            }
//        } catch (FileNotFoundException e) {
//            return false;
//        } catch (IOException e) {
//            return false;
//        }
//    }

    /**
     * Save the document and invoke save on document provider to upload the file
     * to the cloud if necessary.
     */
//    public void saveDocument() {
//        final long lastModified = mInputFile.lastModified();
//        final Activity activity = LibreOfficeMainActivity.this;
//        Toast.makeText(this, R.string.message_saving, Toast.LENGTH_SHORT).show();
//        // local save
//        LOKitShell.sendEvent(new LOEvent(LOEvent.UNO_COMMAND, ".uno:Save"));
//
//        final AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
//            @Override
//            protected Void doInBackground(Void... params) {
//                try {
//                    // rebuild the IFile object from the data passed in the Intent
//                    IFile mStorageFile = DocumentProviderFactory.getInstance()
//                            .getProvider(providerId).createFromUri(documentUri);
//                    // call document provider save operation
//                    mStorageFile.saveDocument(mInputFile);
//                }
//                catch (final RuntimeException e) {
//                    activity.runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            Toast.makeText(activity, e.getMessage(),
//                                    Toast.LENGTH_SHORT).show();
//                        }
//                    });
//                    Log.e(LOGTAG, e.getMessage(), e.getCause());
//                }
//                return null;
//            }
//
//            @Override
//            protected void onPostExecute(Void param) {
//                Toast.makeText(activity, R.string.message_saved,
//                        Toast.LENGTH_SHORT).show();
//            }
//        };
//        // Delay the call to document provider save operation and check the
//        // modification time periodically to ensure the local file has been saved.
//        // TODO: ideally the save operation should have a callback
//        Runnable runTask = new Runnable() {
//            private int timesRun = 0;
//
//            @Override
//            public void run() {
//                if (lastModified < mInputFile.lastModified()) {
//                    // we are sure local save is complete, push changes to cloud
//                    task.execute();
//                }
//                else {
//                    timesRun++;
//                    if(timesRun < 4) {
//                        new Handler().postDelayed(this, 5000);
//                    }
//                    else {
//                        // 20 seconds later, the local file has not changed,
//                        // maybe there were no changes at all
//                        Toast.makeText(activity, R.string.message_save_incomplete, Toast.LENGTH_LONG).show();
//                    }
//                }
//            }
//        };
//        new Handler().postDelayed(runTask, 5000);
//    }

//    @Override
    public void onResume() {
//        super.onResume();
        Log.i(LOGTAG, "onResume..");
        // check for config change
        boolean bEnableExperimental = PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean(ENABLE_EXPERIMENTAL_PREFS_KEY, false);
        if (bEnableExperimental != mIsExperimentalMode) {
            mIsExperimentalMode = bEnableExperimental;
        }
    }

//    @Override
    public void onPause() {
        Log.i(LOGTAG, "onPause..");
//        super.onPause();
    }

//    @Override
    public void onStart() {
        Log.i(LOGTAG, "onStart..");
//        super.onStart();
        MainLOKitShell.sendLoadEvent(mInputFile.getPath());
    }

    public void onStartSize(int renderWidth, int renderHeight) {
        MainOffice.sendBroadCast(PARSE_START);
        MainLOKitShell.sendLoadEvent(mInputFile.getPath(), renderWidth, renderHeight);
    }

//    @Override
    public void onStop() {
        Log.i(LOGTAG, "onStop..");
//        hideSoftKeyboardDirect();
        MainLOKitShell.sendCloseEvent();
//        super.onStop();
    }

//    @Override
    public void onDestroy() {
        Log.i(LOGTAG, "onDestroy..");
        mLayerClient.destroy();
//        super.onDestroy();

//        if (isFinishing()) { // Not an orientation change
//            if (mTempFile != null) {
//                // noinspection ResultOfMethodCallIgnored
//                mTempFile.delete();
//            }
//        }
    }

    public MainLOKitThread getLOKitThread() {
        return sLOKitThread;
    }

//    public List<DocumentPartView> getDocumentPartView() {
//        return mDocumentPartView;
//    }

//    public void disableNavigationDrawer() {
        // Only the original thread that created mDrawerLayout should touch its views.
//        LOKitShell.getMainHandler().post(new Runnable() {
//            @Override
//            public void run() {
//                mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, mDrawerList);
//            }
//        });
//    }

//    public DocumentPartViewListAdapter getDocumentPartViewListAdapter() {
//        return mDocumentPartViewListAdapter;
//    }

    /**
     * Show software keyboard.
     * Force the request on main thread.
     */


//    public void showSoftKeyboard() {
//
//        LOKitShell.getMainHandler().post(new Runnable() {
//            @Override
//            public void run() {
//                if(!isKeyboardOpen) showSoftKeyboardDirect();
//                else hideSoftKeyboardDirect();
//            }
//        });
//
//    }

//    private void showSoftKeyboardDirect() {
//        LayerView layerView = (LayerView) findViewById(R.id.layer_view);
//
//        if (layerView.requestFocus()) {
//            InputMethodManager inputMethodManager = (InputMethodManager) getApplicationContext().getSystemService(Context.INPUT_METHOD_SERVICE);
//            inputMethodManager.showSoftInput(layerView, InputMethodManager.SHOW_FORCED);
//        }
//        isKeyboardOpen=true;
//        hideBottomToolbar();
//    }

//    public void showSoftKeyboardOrFormattingToolbar() {
//        LOKitShell.getMainHandler().post(new Runnable() {
//            @Override
//            public void run() {
//                Toolbar toolbarBottom = (Toolbar) findViewById(R.id.toolbar_bottom);
//                if (toolbarBottom.getVisibility() != View.VISIBLE) {
//                    showSoftKeyboardDirect();
//                }
//            }
//        });
//    }

    /**
     * Hides software keyboard on UI thread.
     */
//    public void hideSoftKeyboard() {
//        LOKitShell.getMainHandler().post(new Runnable() {
//            @Override
//            public void run() {
//                hideSoftKeyboardDirect();
//            }
//        });
//    }

    /**
     * Hides software keyboard.
     */
//    private void hideSoftKeyboardDirect() {
//        if (getCurrentFocus() != null) {
//            InputMethodManager inputMethodManager = (InputMethodManager) getApplicationContext().getSystemService(Context.INPUT_METHOD_SERVICE);
//            inputMethodManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
//            isKeyboardOpen=false;
//        }
//    }

//    public void showBottomToolbar() {
//        LOKitShell.getMainHandler().post(new Runnable() {
//            @Override
//            public void run() {
//                findViewById(R.id.toolbar_bottom).setVisibility(View.VISIBLE);
//            }
//        });
//    }

//    public void hideBottomToolbar() {
//        LOKitShell.getMainHandler().post(new Runnable() {
//            @Override
//            public void run() {
//                findViewById(R.id.toolbar_bottom).setVisibility(View.GONE);
//                findViewById(R.id.formatting_toolbar).setVisibility(View.GONE);
//                findViewById(R.id.search_toolbar).setVisibility(View.GONE);
//            }
//        });
//    }

//    public void showFormattingToolbar() {
//        LOKitShell.getMainHandler().post(new Runnable() {
//            @Override
//            public void run() {
//                showBottomToolbar();
//                findViewById(R.id.formatting_toolbar).setVisibility(View.VISIBLE);
//                findViewById(R.id.search_toolbar).setVisibility(View.GONE);
//                hideSoftKeyboardDirect();
//            }
//        });
//    }

//    public void hideFormattingToolbar() {
//        LOKitShell.getMainHandler().post(new Runnable() {
//            @Override
//            public void run() {
//                hideBottomToolbar();
//                findViewById(R.id.formatting_toolbar).setVisibility(View.GONE);
//            }
//        });
//    }
//
//    public void showSearchToolbar() {
//        LOKitShell.getMainHandler().post(new Runnable() {
//            @Override
//            public void run() {
//                showBottomToolbar();
//                findViewById(R.id.formatting_toolbar).setVisibility(View.GONE);
//                findViewById(R.id.search_toolbar).setVisibility(View.VISIBLE);
//                hideSoftKeyboardDirect();
//            }
//        });
//    }
//
//    public void hideSearchToolbar() {
//        LOKitShell.getMainHandler().post(new Runnable() {
//            @Override
//            public void run() {
//                hideBottomToolbar();
//                findViewById(R.id.search_toolbar).setVisibility(View.GONE);
//            }
//        });
//    }

//    public void showProgressSpinner() {
//        findViewById(R.id.loadingPanel).setVisibility(View.VISIBLE);
//    }

//    public void hideProgressSpinner() {
//        findViewById(R.id.loadingPanel).setVisibility(View.GONE);
//    }

//    public void showAlertDialog(String message) {
//
//        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(LibreOfficeMainActivity.this);
//
//        alertDialogBuilder.setTitle("Error");
//        alertDialogBuilder.setMessage(message);
//        alertDialogBuilder.setNeutralButton("OK", new DialogInterface.OnClickListener() {
//            public void onClick(DialogInterface dialog, int id) {
//                finish();
//            }
//        });
//
//        AlertDialog alertDialog = alertDialogBuilder.create();
//        alertDialog.show();
//    }

//    public DocumentOverlay getDocumentOverlay() {
//        return mDocumentOverlay;
//    }

//    public ToolbarController getToolbarController() {
//        return mToolbarController;
//    }

//    public FontController getFontController() {
//        return mFontController;
//    }

//    public FormattingController getFormattingController() {
//        return mFormattingController;
//    }

//    public void openDrawer() {
//        mDrawerLayout.openDrawer(mDrawerList);
//    }
//
//    public void showAbout() {
//        mAbout.showAbout();
//    }

//    public void showSettings() {
//        startActivity(new Intent(getApplicationContext(), SettingsActivity.class));
//    }

//    public boolean isDrawerEnabled() {
//        boolean isDrawerOpen = mDrawerLayout.isDrawerOpen(mDrawerList);
//        boolean isDrawerLocked = mDrawerLayout.getDrawerLockMode(mDrawerList) != DrawerLayout.LOCK_MODE_UNLOCKED;
//        return !isDrawerOpen && !isDrawerLocked;
//    }

//    private class DocumentPartClickListener implements AdapterView.OnItemClickListener {
//        @Override
//        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//            DocumentPartView partView = mDocumentPartViewListAdapter.getItem(position);
//            MainLOKitShell.sendChangePartEvent(partView.partIndex);
//            mDrawerLayout.closeDrawer(mDrawerList);
//        }
//    }

    private static boolean copyFromAssets(AssetManager assetManager,
                                          String fromAssetPath, String targetDir) {
        try {
            String[] files = assetManager.list(fromAssetPath);

            boolean res = true;
            for (String file : files) {
                String[] dirOrFile = assetManager.list(fromAssetPath + "/" + file);
                if ( dirOrFile.length == 0) {
                    // noinspection ResultOfMethodCallIgnored
                    new File(targetDir).mkdirs();
                    res &= copyAsset(assetManager,
                            fromAssetPath + "/" + file,
                            targetDir + "/" + file);
                } else
                    res &= copyFromAssets(assetManager,
                            fromAssetPath + "/" + file,
                            targetDir + "/" + file);
            }
            return res;
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(LOGTAG, "copyFromAssets failed: " + e.getMessage());
            return false;
        }
    }

    private static boolean copyAsset(AssetManager assetManager, String fromAssetPath, String toPath) {
        ReadableByteChannel source = null;
        FileChannel dest = null;
        try {
            try {
                source = Channels.newChannel(assetManager.open(fromAssetPath));
                dest = new FileOutputStream(toPath).getChannel();
                long bytesTransferred = 0;
                // might not copy all at once, so make sure everything gets copied....
                ByteBuffer buffer = ByteBuffer.allocate(4096);
                while (source.read(buffer) > 0) {
                    buffer.flip();
                    bytesTransferred += dest.write(buffer);
                    buffer.clear();
                }
                Log.v(LOGTAG, "Success copying " + fromAssetPath + " to " + toPath + " bytes: " + bytesTransferred);
                return true;
            } finally {
                if (dest != null) dest.close();
                if (source != null) source.close();
            }
        } catch (FileNotFoundException e) {
            Log.e(LOGTAG, "file " + fromAssetPath + " not found! " + e.getMessage());
            return false;
        } catch (IOException e) {
            Log.e(LOGTAG, "failed to copy file " + fromAssetPath + " from assets to " + toPath + " - " + e.getMessage());
            return false;
        }
    }
}