package tipz.browservio.broha.icons;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

@Dao
public interface IconHashDao {
    @Query("SELECT * FROM iconHash WHERE id LIKE :id LIMIT 1")
    IconHash findById(int id);

    @Query("SELECT * FROM iconHash WHERE iconHash LIKE :hash LIMIT 1")
    IconHash findByHash(String hash);

    @Insert
    void insertAll(IconHash... iconHash);

    @Update
    void updateBroha(IconHash... iconHash);
}
