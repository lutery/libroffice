package org.libreoffice;

import org.libreoffice.kit.LibreOfficeKit;
import org.mozilla.gecko.gfx.GeckoLayerClient;

/**
 * Created by 辉 on 2017/1/10.
 */

public class MainTileProviderFactory {
    private MainTileProviderFactory() {
    }

    public static void initialize() {
        LibreOfficeKit.initializeLibrary();
    }

    public static TileProvider create(GeckoLayerClient layerClient, InvalidationHandler invalidationHandler, String filename) {
        return new MainLOKitTileProvider(layerClient, invalidationHandler, filename);
    }

    public static TileProvider create(GeckoLayerClient layerClient, InvalidationHandler invalidationHandler, String filename, int renderWidth, int renderHeight){
        return new MainLOKitTileProvider(layerClient, invalidationHandler, filename, renderWidth, renderHeight);
    }
}
