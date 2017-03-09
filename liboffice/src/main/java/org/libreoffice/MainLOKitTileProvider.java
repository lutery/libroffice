package org.libreoffice;

import android.graphics.Bitmap;
import android.graphics.PointF;
import android.os.Environment;
import android.util.Log;
import android.view.KeyEvent;

import org.libreoffice.kit.DirectBufferAllocator;
import org.libreoffice.kit.Document;
import org.libreoffice.kit.LibreOfficeKit;
import org.libreoffice.kit.Office;
import org.mozilla.gecko.gfx.BufferedCairoImage;
import org.mozilla.gecko.gfx.CairoImage;
import org.mozilla.gecko.gfx.GeckoLayerClient;
import org.mozilla.gecko.gfx.IntSize;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;

/**
 * Created by 辉 on 2017/1/10.
 */

public class MainLOKitTileProvider implements TileProvider {
    private static final String LOGTAG = MainLOKitTileProvider.class.getSimpleName();
    private static int TILE_SIZE = 256;
    private final GeckoLayerClient mLayerClient;
    private final float mTileWidth;
    private final float mTileHeight;
    private final String mInputFile;
    private Office mOffice;
    private Document mDocument;
    private boolean mIsReady = false;

    private float mDPI;
    private float mWidthTwip;
    private float mHeightTwip;
    private int mRenderWidth;
    private int mRenderHeight;

    private Document.MessageCallback mMessageCallback;

    private long objectCreationTime = System.currentTimeMillis();

    /**
     * Initialize LOKit and load the document.
     * @param layerClient - layerclient implementation
     * @param messageCallback - callback for messages retrieved from LOKit
     * @param input - input path of the document
     */
    public MainLOKitTileProvider(GeckoLayerClient layerClient, Document.MessageCallback messageCallback, String input) {
        mLayerClient = layerClient;
        mMessageCallback = messageCallback;
        mDPI = MainLOKitShell.getDpi();
        mTileWidth = pixelToTwip(TILE_SIZE, mDPI);
        mTileHeight = pixelToTwip(TILE_SIZE, mDPI);

        LibreOfficeKit.putenv("SAL_LOG=+WARN+INFO");
        MainLibreOfficeKit.init(MainOffice.mContext);

        mOffice = new Office(LibreOfficeKit.getLibreOfficeKitHandle());

        mInputFile = input;

        Log.i(LOGTAG, "====> Loading file '" + input + "'");
        mDocument = mOffice.documentLoad(input);

        if (mDocument == null) {
            Log.i(LOGTAG, "====> mOffice.documentLoad() returned null, trying to restart 'Office' and loading again");
            mOffice.destroy();
            Log.i(LOGTAG, "====> mOffice.destroy() done");
            ByteBuffer handle = LibreOfficeKit.getLibreOfficeKitHandle();
            Log.i(LOGTAG, "====> getLibreOfficeKitHandle() = " + handle);
            mOffice = new Office(handle);
            Log.i(LOGTAG, "====> new Office created");
            mDocument = mOffice.documentLoad(input);
        }

        Log.i(LOGTAG, "====> mDocument = " + mDocument);

        if (mDocument != null)
            mDocument.initializeForRendering();

        if (checkDocument()) {
            postLoad();
            mIsReady = true;
        } else {
            mIsReady = false;
        }
    }

    public MainLOKitTileProvider(GeckoLayerClient layerClient, Document.MessageCallback messageCallback, String input, int renderWidth, int renderHeight) {
        mLayerClient = layerClient;
        mMessageCallback = messageCallback;
        mDPI = MainLOKitShell.getDpi();
        mTileWidth = pixelToTwip(TILE_SIZE, mDPI);
        mTileHeight = pixelToTwip(TILE_SIZE, mDPI);
        mRenderHeight = renderHeight;
        mRenderWidth = renderWidth;

        LibreOfficeKit.putenv("SAL_LOG=+WARN+INFO");
        MainLibreOfficeKit.init(MainOffice.mContext);

        mOffice = new Office(LibreOfficeKit.getLibreOfficeKitHandle());

        mInputFile = input;

        Log.i(LOGTAG, "====> Loading file '" + input + "'");
        mDocument = mOffice.documentLoad(input);

        if (mDocument == null) {
            Log.i(LOGTAG, "====> mOffice.documentLoad() returned null, trying to restart 'Office' and loading again");
            mOffice.destroy();
            Log.i(LOGTAG, "====> mOffice.destroy() done");
            ByteBuffer handle = LibreOfficeKit.getLibreOfficeKitHandle();
            Log.i(LOGTAG, "====> getLibreOfficeKitHandle() = " + handle);
            mOffice = new Office(handle);
            Log.i(LOGTAG, "====> new Office created");
            mDocument = mOffice.documentLoad(input);
        }

        Log.i(LOGTAG, "====> mDocument = " + mDocument);

        if (mDocument != null)
            mDocument.initializeForRendering();

        if (checkDocument()) {
            postLoad();
            mIsReady = true;
        } else {
            mIsReady = false;
        }
    }

    /**
     * Triggered after the document is loaded.
     */
    private void postLoad() {
        mDocument.setMessageCallback(mMessageCallback);

        int parts = mDocument.getParts();
        Log.i(LOGTAG, "Document parts: " + parts);

//        LibreOfficeMainActivity.mAppContext.getDocumentPartView().clear();

        // Writer documents always have one part, so hide the navigation drawer.
        if (mDocument.getDocumentType() != Document.DOCTYPE_TEXT) {
            for (int i = 0; i < parts; i++) {
                String partName = mDocument.getPartName(i);
                if (partName.isEmpty()) {
                    partName = getGenericPartName(i);
                }
                Log.i(LOGTAG, "Document part " + i + " name:'" + partName + "'");

                mDocument.setPart(i);
                resetDocumentSize();
                final DocumentPartView partView = new DocumentPartView(i, partName);
//                LibreOfficeMainActivity.mAppContext.getDocumentPartView().add(partView);
            }
        } else {
//            LibreOfficeMainActivity.mAppContext.disableNavigationDrawer();
        }

        mDocument.setPart(0);

        setupDocumentFonts();

//        LOKitShell.getMainHandler().post(new Runnable() {
//            @Override
//            public void run() {
//                LibreOfficeMainActivity.mAppContext.getDocumentPartViewListAdapter().notifyDataSetChanged();
//            }
//        });
    }

    private void setupDocumentFonts() {
        String values = mDocument.getCommandValues(".uno:CharFontName");
        if (values == null || values.isEmpty())
            return;

//        LOKitShell.getFontController().parseJson(values);
//        LOKitShell.getFontController().setupFontViews();
    }

    private String getGenericPartName(int i) {
        if (mDocument == null) {
            return "";
        }
        switch (mDocument.getDocumentType()) {
            case Document.DOCTYPE_DRAWING:
            case Document.DOCTYPE_TEXT:
                return "Page " + (i + 1);
            case Document.DOCTYPE_SPREADSHEET:
                return "Sheet " + (i + 1);
            case Document.DOCTYPE_PRESENTATION:
                return "Slide " + (i + 1);
            case Document.DOCTYPE_OTHER:
            default:
                return "Part " + (i + 1);
        }
    }

    public static float twipToPixel(float input, float dpi) {
        return input / 1440.0f * dpi;
    }

    public static float pixelToTwip(float input, float dpi) {
        return (input / dpi) * 1440.0f;
    }


    /**
     * @see TileProvider#getPartsCount()
     */
    @Override
    public int getPartsCount() {
        return mDocument.getParts();
    }

    /**
     * @see TileProvider#onSwipeLeft()
     */
    @Override
    public void onSwipeLeft() {
        if (mDocument.getDocumentType() == Document.DOCTYPE_PRESENTATION &&
                getCurrentPartNumber() < getPartsCount()-1) {
            LOKitShell.sendChangePartEvent(getCurrentPartNumber()+1);
        }
    }

    /**
     * @see TileProvider#onSwipeRight()
     */
    @Override
    public void onSwipeRight() {
        if (mDocument.getDocumentType() == Document.DOCTYPE_PRESENTATION &&
                getCurrentPartNumber() > 0) {
            LOKitShell.sendChangePartEvent(getCurrentPartNumber()-1);
        }
    }

    private boolean checkDocument() {
        String error = null;
        boolean ret;

        if (mDocument == null || !mOffice.getError().isEmpty()) {
            error = "Cannot open " + mInputFile + ": " + mOffice.getError();
            ret = false;
        } else {
            ret = resetDocumentSize();
            if (!ret) {
                error = "Document returned an invalid size or the document is empty.";
            }
        }

        if (!ret) {
            final String message = error;
            LOKitShell.getMainHandler().post(new Runnable() {
                @Override
                public void run() {
                    LibreOfficeMainActivity.mAppContext.showAlertDialog(message);
                }
            });
        }

        return ret;
    }

    private boolean resetDocumentSize() {
        mWidthTwip = mDocument.getDocumentWidth();
        mHeightTwip = mDocument.getDocumentHeight();

        if (mWidthTwip == 0 || mHeightTwip == 0) {
            Log.e(LOGTAG, "Document size zero - last error: " + mOffice.getError());
            return false;
        } else {
            Log.i(LOGTAG, "Reset document size: " + mDocument.getDocumentWidth() + " x " + mDocument.getDocumentHeight());
        }

        return true;
    }

    /**
     * @see TileProvider#getPageWidth()
     */
    @Override
    public int getPageWidth() {
        return (int) twipToPixel(mWidthTwip, mDPI);
    }

    /**
     * @see TileProvider#getPageHeight()
     */
    @Override
    public int getPageHeight() {
        return (int) twipToPixel(mHeightTwip, mDPI);
    }

    /**
     * @see TileProvider#isReady()
     */
    @Override
    public boolean isReady() {
        return mIsReady;
    }

    /**
     * @see TileProvider#createTile(float, float, IntSize, float)
     */
    @Override
    public CairoImage createTile(float x, float y, IntSize tileSize, float zoom) {
        ByteBuffer buffer = DirectBufferAllocator.guardedAllocate(tileSize.width * tileSize.height * 4);
        if (buffer == null)
            return null;

        CairoImage image = new BufferedCairoImage(buffer, tileSize.width, tileSize.height, CairoImage.FORMAT_ARGB32);
        rerenderTile(image, x, y, tileSize, zoom);
        return image;
    }

    public Bitmap createBlankBitmap(int width, int height){
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        int[] pix = new int[(width * height)];
        /** 创建一个白色背景 */
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int index = y * width + x;
                int r = ((pix[index] >> 16) & 0xff) | 0xff;
                int g = ((pix[index] >> 8) & 0xff) | 0xff;
                int b = (pix[index] & 0xff) | 0xff;
                pix[index] = 0xff000000 | (r << 16) | (g << 8) | b;
            }
        }
        bitmap.setPixels(pix, 0, width, 0, 0, width, height);

        return bitmap;
    }

    public long renderTileHeight(){
        long freeMemSize = (long)((Runtime.getRuntime().maxMemory() - Runtime.getRuntime().totalMemory() + Runtime.getRuntime().freeMemory()) * 0.3);
        long bytePerLine = this.getPageWidth() * 4;
        long singlePageHeight = freeMemSize / bytePerLine;

        if (singlePageHeight == 0){
            return 0;
        }

        return singlePageHeight > this.getPageHeight() ? this.getPageHeight() : singlePageHeight;
    }

    public int renderTileHeight(int renderWidth){
        long pageWidth = this.getPageWidth();
        long pageHeight = this.getPageHeight();

        float pageRatio = (float)pageWidth / (float)pageHeight;
        float renderRatio = (float)mRenderWidth / (float)mRenderHeight;

//        float dValue = pageRatio - renderRatio;
        if ((pageRatio - renderRatio) < -0.1){
            return mRenderHeight;
        }
        else{
            return (int)(renderWidth / pageRatio);
        }
    }

    public File createRenderTile(int index) throws IOException {
        File tempDirs = new File(Environment.getExternalStoragePublicDirectory("StartIPrint").getAbsolutePath() + "/RenderTemp");
        if (!tempDirs.exists()){
            tempDirs.mkdirs();
        }

        File tempFile = new File(tempDirs.getAbsolutePath() + "/renderfile_" + mDocument.getPart() + "_" + index + ".jpg");
        if (tempFile.exists()){
            tempFile.delete();
        }

        tempFile.createNewFile();

        return tempFile;
    }

//    static int i = 0;

    public void renderTileWidth(float zoom){
        Log.d(LOGTAG, "MainLOKitTileProvider renderTileWidth");
        ArrayList<String> parseFileList = new ArrayList<>();

        try {
//            if (i == 0) {
            int documentType = mDocument.getDocumentType();//word = 0, excel = 1, pptx = 2

            for (int iPart = 0; iPart < mDocument.getParts(); ++iPart) {
                long pageWidth = this.getPageWidth();
                long pageHeight = this.getPageHeight();

                float pageRatio = (float) pageWidth / (float) pageHeight;
                float renderRatio = (float) mRenderWidth / (float) mRenderHeight;

                int renderPageHeight = mRenderHeight;
                if (documentType == 1) {
                    int renderPageWidth = mRenderWidth;

                    if ((pageRatio < 1 && renderRatio > 1) || (pageRatio > 1 && renderRatio < 1)){
                        renderPageHeight = this.mRenderWidth;
                        renderPageWidth = this.mRenderHeight;
                    }
                    // 竖直方向
//                    if ((pageRatio < 1 && renderRatio < 1) || (pageRatio > 1 && renderRatio > 1)){
                        float widthRatio = (float)pageWidth / (float)renderPageWidth;
                        int scaleRenderHeight = (int)(renderPageHeight * widthRatio);
                        if (scaleRenderHeight > pageHeight){
//                            renderPageHeight = mRenderHeight;
                            renderPageWidth = (int)(renderPageHeight * pageRatio);
                        }
                        else{
//                            renderPageHeight = mRenderHeight;
//                            renderPageWidth = mRenderWidth;
                            renderPageHeight = (int)(renderPageWidth / pageRatio);
                        }

                        ByteBuffer buffer = DirectBufferAllocator.guardedAllocate(renderPageWidth * renderPageHeight * 4);
                        CairoImage testImage = new BufferedCairoImage(buffer, renderPageWidth, renderPageHeight, CairoImage.FORMAT_ARGB32);
                        mDocument.paintTile(testImage.getBuffer(), renderPageWidth, renderPageHeight, 0, 0, (int) (mWidthTwip), (int) (mHeightTwip));

                        File file = createRenderTile(0);

                        Bitmap bitmap = createBlankBitmap(renderPageWidth, renderPageHeight);
                        bitmap.copyPixelsFromBuffer(testImage.getBuffer());
                        OutputStream outputStream = new FileOutputStream(file);
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
                        outputStream.flush();
                        outputStream.close();
                        bitmap.recycle();

                    parseFileList.add(file.getAbsolutePath());
//                    }
//                    // 横向
//                    else{
//
//                    }
                } else {
                    if ((pageRatio - renderRatio) > -0.1) {
                        renderPageHeight = (int) (mRenderWidth / pageRatio);

                        if (renderPageHeight == 0) {
                            return;
                        }

//                    for (long totleHeight = 0, i = 0; totleHeight < pageHeight; totleHeight += renderPageHeight, ++i) {

//                        Log.d("reanderfile", "i" + i);
                        long curHeight = renderPageHeight;
//                        if (((i + 1) * renderPageHeight) > pageHeight) {
//                            curHeight = pageHeight - renderPageHeight * i;
//                        }
                        ByteBuffer buffer = DirectBufferAllocator.guardedAllocate(this.mRenderWidth * (int) curHeight * 4);
                        CairoImage testImage = new BufferedCairoImage(buffer, this.mRenderWidth, (int) curHeight, CairoImage.FORMAT_ARGB32);

//                        float twipY = pixelToTwip((renderPageHeight * i), mDPI) / zoom;
                        float twipY = 0;
//                        float twipHeight = pixelToTwip(curHeight, mDPI) / zoom;
                        mDocument.paintTile(testImage.getBuffer(), this.mRenderWidth, (int) curHeight, 0, (int) twipY, (int) (mWidthTwip), (int) (mHeightTwip));

                        File file = createRenderTile(0);

                        int width = this.mRenderWidth;
                        Bitmap bitmap = createBlankBitmap(width, (int) curHeight);
                        bitmap.copyPixelsFromBuffer(testImage.getBuffer());
                        OutputStream outputStream = new FileOutputStream(file);
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
                        outputStream.flush();
                        outputStream.close();
                        bitmap.recycle();

                        parseFileList.add(file.getAbsolutePath());
//            }
//                    }
                    } else {
                        if (renderPageHeight == 0) {
                            return;
                        }
                        // renderPageHeight:绘制的图片高度
                        // curHeight = renderPageHeight
                        // curWidth：绘制图片的宽度，等于this.mRenderWidth
                        // scaleRenderHeight：根据比率，换算到实际pageWidth的绘制高度

                        float widthRatio = (float) pageWidth / (float) mRenderWidth;
                        int scaleRenderHeight = (int) (mRenderHeight * widthRatio);
                        int printPart = (int) (pageHeight / scaleRenderHeight);
                        printPart += ((pageHeight % scaleRenderHeight) == 0 ? 0 : 1);
                        int saclePageHeight = (int) (pageHeight / widthRatio);

//                    for (long totleHeight = 0, i = 0; totleHeight < pageHeight; totleHeight += renderPageHeight, ++i) {

                        for (long iPPart = 0, i = 0; iPPart < printPart; ++iPPart, ++i) {

                            Log.d("reanderfile", "i" + i);
                            long curHeight = renderPageHeight;
                            int curWidth = this.mRenderWidth;

                            float twipY = pixelToTwip((scaleRenderHeight * i), mDPI) / zoom;
                            float twipHeight = pixelToTwip(scaleRenderHeight, mDPI) / zoom;
                            if (((i + 1) * renderPageHeight) > saclePageHeight) {
                                curHeight = saclePageHeight - renderPageHeight * i;
                                twipHeight = pixelToTwip(pageHeight - scaleRenderHeight * i,
                                        mDPI) / zoom;
                            }
                            ByteBuffer buffer = DirectBufferAllocator.guardedAllocate(curWidth * (int) curHeight * 4);
                            CairoImage testImage = new BufferedCairoImage(buffer, curWidth, (int) curHeight, CairoImage.FORMAT_ARGB32);

                            mDocument.paintTile(testImage.getBuffer(), curWidth, (int) curHeight,
                                    0, (int) twipY, (int) (mWidthTwip), (int) (twipHeight));

                            File file = createRenderTile((int) i);

                            Bitmap bitmap = createBlankBitmap(curWidth, (int) curHeight);
                            bitmap.copyPixelsFromBuffer(testImage.getBuffer());
                            OutputStream outputStream = new FileOutputStream(file);
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
                            outputStream.flush();
                            outputStream.close();
                            bitmap.recycle();

                            parseFileList.add(file.getAbsolutePath());
//            }
                        }
                    }

                    if (documentType == 0) {
                        break;
                    }

                    changePart(iPart);
                }
            }

            changePart(0);
            MainOffice.sendBroadCast(MainOffice.PARSE_END, parseFileList);
        }
        catch (IOException ioe){
            ioe.printStackTrace();
            MainOffice.sendBroadCast(MainOffice.PARSE_ERROR);
        }
    }

    public void reanderTile(float zoom) {
        Log.d(LOGTAG, "MainLOKitTileProvider reanderTile");

        try {
//            if (i == 0) {
            for (int iPart = 0; iPart < mDocument.getParts(); ++iPart) {
                long renderPageHeight = this.renderTileHeight();
                long pageHeight = this.getPageHeight();

                if (renderPageHeight == 0) {
                    return;
                }

                for (long totleHeight = 0, i = 0; totleHeight < pageHeight; totleHeight += renderPageHeight, ++i) {

                    Log.d("reanderfile", "i" + i);
                    long curHeight = renderPageHeight;
                    if (((i + 1) * renderPageHeight) > pageHeight) {
                        curHeight = pageHeight - renderPageHeight * i;
                    }
                    ByteBuffer buffer = DirectBufferAllocator.guardedAllocate(this.getPageWidth() * (int) curHeight * 4);
                    CairoImage testImage = new BufferedCairoImage(buffer, this.getPageWidth(), (int) curHeight, CairoImage.FORMAT_ARGB32);

                    float twipY = pixelToTwip((renderPageHeight * i), mDPI) / zoom;
                    float twipHeight = pixelToTwip(curHeight, mDPI) / zoom;
                    mDocument.paintTile(testImage.getBuffer(), this.getPageWidth(), (int) curHeight, 0, (int) twipY, (int) (mWidthTwip), (int) (twipHeight));

//
//                File file = new File("/sdcard/renderTile.png");
////                i++;
//                if (file.exists()) {
//                    file.delete();
//                }
//                file.createNewFile();

                    File file = createRenderTile((int) i);

                    int width = this.getPageWidth();
//                int height = this.getPageHeight();
                    Bitmap bitmap = createBlankBitmap(width, (int) curHeight);
                    bitmap.copyPixelsFromBuffer(testImage.getBuffer());
                    OutputStream outputStream = new FileOutputStream(file);
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
                    outputStream.flush();
                    outputStream.close();
                    bitmap.recycle();
//            }
                }

                changePart(iPart);
            }

            changePart(0);
        }
        catch (IOException ioe){
            ioe.printStackTrace();
        }
    }

    /**
     * @see TileProvider#rerenderTile(CairoImage, float, float, IntSize, float)
     */
    @Override
    public void rerenderTile(CairoImage image, float x, float y, IntSize tileSize, float zoom) {
        if (mDocument != null && image.getBuffer() != null) {
            float twipX = pixelToTwip(x, mDPI) / zoom;
            float twipY = pixelToTwip(y, mDPI) / zoom;
            float twipWidth = mTileWidth / zoom;
            float twipHeight = mTileHeight / zoom;
            long start = System.currentTimeMillis() - objectCreationTime;

//            try {
//                if (i == 0) {
//                    ByteBuffer buffer = DirectBufferAllocator.guardedAllocate(this.getPageWidth() * this.getPageHeight() * 4);
//                    CairoImage testImage = new BufferedCairoImage(buffer, this.getPageWidth(), this.getPageHeight(), CairoImage.FORMAT_ARGB32);
//                    mDocument.paintTile(testImage.getBuffer(), this.getPageWidth(), this.getPageHeight(), 0, 0, (int)(this.getPageWidth() / 0.225), (int)(this.getPageHeight() / 0.225));
//
//                    File file = new File("/sdcard/test" + i + ".png");
//                    i++;
//                    if (file.exists()) {
//                        file.delete();
//                    }
//                    file.createNewFile();
//
//                    int width = this.getPageWidth();
//                    int height = this.getPageHeight();
//                    Bitmap bitmap = createBlankBitmap(width, height);
//                    bitmap.copyPixelsFromBuffer(testImage.getBuffer());
//                    OutputStream outputStream = new FileOutputStream(file);
//                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
//                }
//            }
//            catch (Exception e){
//                e.printStackTrace();
//            }


            //Log.i(LOGTAG, "paintTile >> @" + start + " (" + tileSize.width + " " + tileSize.height + " " + (int) twipX + " " + (int) twipY + " " + (int) twipWidth + " " + (int) twipHeight + ")");
            mDocument.paintTile(image.getBuffer(), tileSize.width, tileSize.height, (int) twipX, (int) twipY, (int) twipWidth, (int) twipHeight);

//            try {
//
//                File file = new File("/sdcard/test" + i + ".png");
//                i++;
//                if (file.exists()){
//                    file.delete();
//                }
//                file.createNewFile();
//
//                Bitmap bitmap = createBlankBitmap(tileSize.width, tileSize.height);
//                bitmap.copyPixelsFromBuffer(image.getBuffer());
//                OutputStream outputStream = new FileOutputStream(file);
//                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
//            }
//            catch (Exception e){
//                e.printStackTrace();
//            }

            long stop = System.currentTimeMillis() - objectCreationTime;
            //Log.i(LOGTAG, "paintTile << @" + stop + " elapsed: " + (stop - start));
        } else {
            if (mDocument == null) {
                Log.e(LOGTAG, "Document is null!!");
            }
        }
    }

    /**
     * @see TileProvider#thumbnail(int)
     */
    @Override
    public Bitmap thumbnail(int size) {
        int widthPixel = getPageWidth();
        int heightPixel = getPageHeight();

        if (widthPixel > heightPixel) {
            double ratio = heightPixel / (double) widthPixel;
            widthPixel = size;
            heightPixel = (int) (widthPixel * ratio);
        } else {
            double ratio = widthPixel / (double) heightPixel;
            heightPixel = size;
            widthPixel = (int) (heightPixel * ratio);
        }

        Log.w(LOGTAG, "Thumbnail size: " + getPageWidth() + " " + getPageHeight() + " " + widthPixel + " " + heightPixel);

        ByteBuffer buffer = ByteBuffer.allocateDirect(widthPixel * heightPixel * 4);
        if (mDocument != null)
            mDocument.paintTile(buffer, widthPixel, heightPixel, 0, 0, (int) mWidthTwip, (int) mHeightTwip);

        Bitmap bitmap = Bitmap.createBitmap(widthPixel, heightPixel, Bitmap.Config.ARGB_8888);
        bitmap.copyPixelsFromBuffer(buffer);
        if (bitmap == null) {
            Log.w(LOGTAG, "Thumbnail not created!");
        }
        return bitmap;
    }

    /**
     * @see TileProvider#close()
     */
    @Override
    public void close() {
        Log.i(LOGTAG, "Document destroyed: " + mInputFile);
        if (mDocument != null) {
            mDocument.destroy();
            mDocument = null;
        }
    }

    /**
     * @see TileProvider#isTextDocument()
     */
    @Override
    public boolean isTextDocument() {
        return mDocument != null && mDocument.getDocumentType() == Document.DOCTYPE_TEXT;
    }

    /**
     * @see TileProvider#isSpreadsheet()
     */
    @Override
    public boolean isSpreadsheet() {
        return mDocument != null && mDocument.getDocumentType() == Document.DOCTYPE_SPREADSHEET;
    }

    /**
     * Returns the Unicode character generated by this event or 0.
     */
    private int getCharCode(KeyEvent keyEvent) {
        switch (keyEvent.getKeyCode())
        {
            case KeyEvent.KEYCODE_DEL:
            case KeyEvent.KEYCODE_ENTER:
                return 0;
        }
        return keyEvent.getUnicodeChar();
    }

    /**
     * Returns the integer code representing the key of the event (non-zero for
     * control keys).
     */
    private int getKeyCode(KeyEvent keyEvent) {
        switch (keyEvent.getKeyCode()) {
            case KeyEvent.KEYCODE_DEL:
                return com.sun.star.awt.Key.BACKSPACE;
            case KeyEvent.KEYCODE_ENTER:
                return com.sun.star.awt.Key.RETURN;
        }
        return 0;
    }

    /**
     * @see TileProvider#sendKeyEvent(KeyEvent)
     */
    @Override
    public void sendKeyEvent(KeyEvent keyEvent) {
        if (keyEvent.getAction() == KeyEvent.ACTION_MULTIPLE) {
            String keyString = keyEvent.getCharacters();
            for (int i = 0; i < keyString.length(); i++) {
                int codePoint = keyString.codePointAt(i);
                mDocument.postKeyEvent(Document.KEY_EVENT_PRESS, codePoint, getKeyCode(keyEvent));
            }
        } else if (keyEvent.getAction() == KeyEvent.ACTION_DOWN) {
            mDocument.postKeyEvent(Document.KEY_EVENT_PRESS, getCharCode(keyEvent), getKeyCode(keyEvent));
        } else if (keyEvent.getAction() == KeyEvent.ACTION_UP) {
            mDocument.postKeyEvent(Document.KEY_EVENT_RELEASE, getCharCode(keyEvent), getKeyCode(keyEvent));
        }
    }

    private void mouseButton(int type, PointF inDocument, int numberOfClicks, float zoomFactor) {
        int x = (int) pixelToTwip(inDocument.x, mDPI);
        int y = (int) pixelToTwip(inDocument.y, mDPI);

        mDocument.setClientZoom(TILE_SIZE, TILE_SIZE, (int) (mTileWidth / zoomFactor), (int) (mTileHeight / zoomFactor));
        mDocument.postMouseEvent(type, x, y, numberOfClicks, Document.MOUSE_BUTTON_LEFT, Document.KEYBOARD_MODIFIER_NONE);
    }

    /**
     * @see TileProvider#mouseButtonDown(PointF, int)
     */
    @Override
    public void mouseButtonDown(PointF documentCoordinate, int numberOfClicks, float zoomFactor) {
        mouseButton(Document.MOUSE_EVENT_BUTTON_DOWN, documentCoordinate, numberOfClicks, zoomFactor);
    }

    /**
     * @see TileProvider#mouseButtonUp(PointF, int)
     */
    @Override
    public void mouseButtonUp(PointF documentCoordinate, int numberOfClicks, float zoomFactor) {
        mouseButton(Document.MOUSE_EVENT_BUTTON_UP, documentCoordinate, numberOfClicks, zoomFactor);
    }

    /**
     * @param command   UNO command string
     * @param arguments Arguments to UNO command
     */
    @Override
    public void postUnoCommand(String command, String arguments) {
        mDocument.postUnoCommand(command, arguments);
    }

    private void setTextSelection(int type, PointF documentCoordinate) {
        int x = (int) pixelToTwip(documentCoordinate.x, mDPI);
        int y = (int) pixelToTwip(documentCoordinate.y, mDPI);
        mDocument.setTextSelection(type, x, y);
    }

    /**
     * @see TileProvider#setTextSelectionStart(PointF)
     */
    @Override
    public void setTextSelectionStart(PointF documentCoordinate) {
        setTextSelection(Document.SET_TEXT_SELECTION_START, documentCoordinate);
    }

    /**
     * @see TileProvider#setTextSelectionEnd(PointF)
     */
    @Override
    public void setTextSelectionEnd(PointF documentCoordinate) {
        setTextSelection(Document.SET_TEXT_SELECTION_END, documentCoordinate);
    }

    /**
     * @see TileProvider#setTextSelectionReset(PointF)
     */
    @Override
    public void setTextSelectionReset(PointF documentCoordinate) {
        setTextSelection(Document.SET_TEXT_SELECTION_RESET, documentCoordinate);
    }

    /**
     * @see TileProvider#setGraphicSelectionStart(PointF)
     */
    @Override
    public void setGraphicSelectionStart(PointF documentCoordinate) {
        setGraphicSelection(Document.SET_GRAPHIC_SELECTION_START, documentCoordinate);
    }

    /**
     * @see TileProvider#setGraphicSelectionEnd(PointF)
     */
    @Override
    public void setGraphicSelectionEnd(PointF documentCoordinate) {
        setGraphicSelection(Document.SET_GRAPHIC_SELECTION_END, documentCoordinate);
    }

    private void setGraphicSelection(int type, PointF documentCoordinate) {
        int x = (int) pixelToTwip(documentCoordinate.x, mDPI);
        int y = (int) pixelToTwip(documentCoordinate.y, mDPI);
        mDocument.setGraphicSelection(type, x, y);
    }

    @Override
    protected void finalize() throws Throwable {
        close();
        super.finalize();
    }

    /**
     * @see TileProvider#changePart(int)
     */
    @Override
    public void changePart(int partIndex) {
        if (mDocument == null)
            return;

        mDocument.setPart(partIndex);
        resetDocumentSize();
    }

    /**
     * @see TileProvider#getCurrentPartNumber()
     */
    @Override
    public int getCurrentPartNumber() {
        if (mDocument == null)
            return 0;

        return mDocument.getPart();
    }
}
