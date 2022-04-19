package tipz.browservio.broha.icons;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class IconHash {
    @PrimaryKey(autoGenerate = true)
    private int id;

    @ColumnInfo
    private String iconHash;

    public IconHash(int id, String iconHash) {
        this.id = id;
        this.iconHash = iconHash;
    }
}
