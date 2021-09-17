package com.hybridss.databasemanager;


import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.util.Log;

import com.hybridss.utilities.logger.LGLogger;
import com.hybridss.utilities.utilities.utils.UTFileUtilities;

import java.util.Locale;

public class DTDatabase {

    private final String TAG = getClass().getSimpleName();
    public int openCount;
    private SQLiteDatabase mDatabase;
    private String mNameFile;
    public String lastErrorMessage;
    public int errorCode;

    /**
     * Accedemos directamente al archivo por la ruta fisica. Sin usar el context.
     *
     * @param name
     */

    protected DTDatabase(String name) {
        mNameFile = name;
        openCount = 0;
        lastErrorMessage = "";
        errorCode = -1;
    }

    public boolean open() {
        if (!UTFileUtilities.existeArchivo(mNameFile)) {
            throw new IllegalAccessError("No existe la base de datos ");
        }

        if (attempOpen()) {
            return true;
        }
        return false;
    }

    private boolean attempOpen()
    {
        if (mDatabase != null && mDatabase.isOpen()) {
            openCount++;
            return true;
        }

        String strPath = LGLogger.PATH + mNameFile;

        if (mDatabase == null) {
            mDatabase = SQLiteDatabase.openDatabase(strPath, null, 0);

            if (mDatabase == null) {
                throw new IllegalStateException("Wrong to access database ");
            }

            openCount++;
            return true;
        }

        if (mDatabase != null)
        {
            if (!mDatabase.isOpen())
            {
                mDatabase = SQLiteDatabase.openDatabase(strPath, null, 0);
                if (mDatabase.isOpen())
                {
                    openCount++;
                }
            }
        }
        return true;
    }

    public boolean close()
    {
        if (openCount > 0) {
            openCount--;
        }

        if (openCount == 0) {
            mDatabase.close();
        }

        return true;
    }

    public Cursor executeQuery(String query)
    {
        Cursor c = null;
        try {
            c = mDatabase.rawQuery(query, null);
        } catch (SQLiteException ex) {
            Log.e(TAG, "--->>>>>   " + ex.toString());
        }
        return c;
    }

    public boolean insertar(String tabla, ContentValues values) throws SQLException
    {
        boolean bSuccess = false;
        try {
            bSuccess = mDatabase.insertOrThrow(tabla, null, values) > 0;
        } catch (SQLiteConstraintException ex)
        {
            //** Evita Log cuando es CatGarantiasDB de registro único. **//
            //if(mNameFile != DTDatabaseManager.CATGARANTIAS_DATABASE && errorCode != 155){
            // Log.e(TAG, ex.getMessage());}
        } catch (SQLException ex) {
            // Log.e(TAG, ex.getMessage());
        } catch (Exception ex) {
            //Log.e(TAG, ex.getMessage());
        }
        return bSuccess;
    }

    public String[] obtenerColumnasTabla(String table)
    {
        String[] columnNames = null;
        try {
            Cursor c = mDatabase.query(table, null, null, null, null, null, null);
            columnNames = c.getColumnNames();
        } catch (Exception ex) {
            LGLogger.e(TAG, ex);
        }
        return columnNames;
    }

    public Cursor select(String querySelect) throws SQLException
    {
        Cursor c = null;
        try {
            c = mDatabase.rawQuery(querySelect, null);
        } catch (Exception ex) {
            LGLogger.e(TAG, ex);
        }
        return c;
    }

    public Cursor selectWithWhere(String querySelect, String[] args)
    {
        Cursor c = null;
        try {
            c = mDatabase.rawQuery(querySelect, args);
        } catch (Exception ex) {
            LGLogger.e(TAG, ex);
        }
        return c;
    }

    public boolean actualizar(String tabla, ContentValues values, String where, String[] args) {
        boolean bSuccess = false;
        try {

            bSuccess = mDatabase.update(tabla, values, where, args) > 0;
        } catch (Exception ex) {
            LGLogger.e(TAG, ex);
        }
        return bSuccess;
    }

    public boolean updateWithValuesWhereArgs(String tabla, ContentValues values, String where, String[] arg)
    {
        boolean b = mDatabase.update(tabla, values, where, arg) > 0;
        //if (!b) {
        //}
        return b;
    }


    public boolean updateWithArgs(String query, String[] args)
    {
        boolean b = false;
        try {
            Cursor c = mDatabase.rawQuery(query, args);
            if (c.moveToFirst()) {
                b = true;
            }
            c.close();
        }
        catch (Exception ex)
        {
            Log.e(TAG, ex.getMessage());
        }
        return b;
    }

    public boolean update(String query)
    {
        try {
            errorCode = -1;
            Cursor c = mDatabase.rawQuery(query, null);
            if (c.moveToFirst()) {
            }
            c.close();
        }
        catch (SQLException | IllegalArgumentException ex)
        {
            lastErrorMessage = ex.getMessage();
            String error = ex.getMessage();
            try {
                String errorCodeString = error.substring(error.indexOf("(") + 6, error.indexOf(")"));
                errorCode = Integer.parseInt(errorCodeString);
            } catch (NumberFormatException numberException) {  errorCode = -1; }
            finally {
                return false;
            }
        }
        return true;
    }

    public boolean delete(String query)
    {
        boolean succes = false;
        try {
            mDatabase.execSQL(query);
            succes = true;
        }
        catch (Exception ex)
        {
            Log.e(TAG, ex.getMessage());
        }

        return succes;
    }

    public boolean commit()
    {
        boolean success = false;

        try {
            mDatabase.setTransactionSuccessful();
            success = true;
        } catch (RuntimeException e) {
            success = false;
        } finally {
            mDatabase.endTransaction();
        }

        return success;
    }

    public boolean rollback()
    {
        boolean success = true;

        try {
            mDatabase.endTransaction();
        } catch (RuntimeException e) {
            success = false;
        }

        return success;
    }

    public boolean beginTransaction()
    {
        boolean success = false;

        try {
            mDatabase.beginTransaction();
            success = true;
        } catch (RuntimeException e) {
            success = false;
        }

        return success;
    }

    public void forzarCerrarBase()
    {
        if (mDatabase!= null && mDatabase.isOpen()) {
            mDatabase.close();
            openCount = 0;
        }
    }

    public Boolean columnExists(String columnName, String tableName)
    {
        boolean returnBool = false;
        try {
            tableName = tableName.toLowerCase();
            columnName = columnName.toLowerCase();
            Cursor rs = getTableSchema(tableName);
            if (rs.moveToFirst()) {
                do {
                    if (rs.getString(rs.getColumnIndex("name")).toLowerCase().equals(columnName)) {
                        returnBool = true;
                        break;
                    }
                } while (rs.moveToNext());
            }
            rs.close();
        }
        catch (Exception exeption){
            LGLogger.e("DTDatabase", "Problema para columnExists " + exeption);
        }
        finally {
            return returnBool;
        }
    }

    private Cursor getTableSchema(String tableName) {
        return executeQuery(String.format(Locale.US, "PRAGMA table_info('%s')", tableName));
    }

    public boolean isOpen(){
        return mDatabase.isOpen();
    }
}
