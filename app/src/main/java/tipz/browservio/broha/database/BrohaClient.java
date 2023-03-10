package tipz.browservio.broha.database;

import android.content.Context;

import androidx.room.Room;

public class BrohaClient {
    private final BrohaDatabase appDatabase;

    public BrohaClient(Context context, String dbName) {
        //appDatabase = Room.databaseBuilder(context, BrohaDatabase.class, dbName).build();
        /* FIXME: Don't run on main thread */
        appDatabase = Room.databaseBuilder(context, BrohaDatabase.class, dbName).allowMainThreadQueries().build();
    }

    public BrohaDao getDao() {
        return appDatabase.brohaDao();
    }
}
