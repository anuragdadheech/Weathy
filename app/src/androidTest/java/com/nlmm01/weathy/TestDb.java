package com.nlmm01.weathy;

import android.database.sqlite.SQLiteDatabase;
import android.test.AndroidTestCase;

import com.nlmm01.weathy.data.WeatherDbHelper;

/**
 * Created by nlmm01 on 31/12/14.
 */
public class TestDb extends AndroidTestCase{
    public void testCreateDb() throws Throwable {
        mContext.deleteDatabase(WeatherDbHelper.DATABASE_NAME);
        SQLiteDatabase db = new WeatherDbHelper(
                this.mContext).getWritableDatabase();
        assertEquals(true, db.isOpen());
        db.close();
    }

    public void testInsertReadDb() {
        
    }
}
