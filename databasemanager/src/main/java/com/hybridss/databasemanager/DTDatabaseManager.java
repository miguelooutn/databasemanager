package com.hybridss.databasemanager;


import android.content.res.AssetManager;

import com.hybridss.utilities.logger.LGLogger;
import com.hybridss.utilities.utilities.utils.UTFileUtilities;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class DTDatabaseManager {
    private static DTDatabase database;
    private static final String TAG = DTDatabaseManager.class.getSimpleName();

    public static DTDatabase database(String nameDatabase) {
        return DTDatabaseManager.sharedInstanceDataBase(nameDatabase);
    }

    private static DTDatabase sharedInstanceDataBase(String nameDatabase) {
        if (database != null) {
            return database;
        }

        if (UTFileUtilities.existeArchivo(nameDatabase)) {
            database = new DTDatabase(nameDatabase);
        } else {
            throw new IllegalArgumentException("No existe la base de datos ");
        }
        return database;
    }

    public static boolean copyDatabaseFromAssets(AssetManager assetManager,String nameDatabase) {
        boolean response = false;

        try {
            response = copiarBaseAssets(assetManager, nameDatabase);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return response;
    }

    private static boolean copiarBaseAssets(AssetManager manager, String nameDatabase) throws IOException {

        if (UTFileUtilities.existeArchivo(nameDatabase)){
            return true;
        }

        boolean sucess = false;
        InputStream is = null;
        OutputStream os = null;

        try {
            is = manager.open(nameDatabase);
            os = new FileOutputStream(LGLogger.PATH + nameDatabase);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
            sucess = true;
        } catch (Exception e) {
            LGLogger.e(TAG, e);
        } finally {
            is.close();
            os.close();
        }
        return sucess;
    }

}
