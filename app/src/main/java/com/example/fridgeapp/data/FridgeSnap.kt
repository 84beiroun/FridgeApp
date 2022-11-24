package com.example.fridgeapp.data

import android.os.Parcel
import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

//невероятно перегруженная сущность
//на самом деле просто имплимент был parcelable, так как в бандл нельзя передавать обычные списки
@Entity
data class FridgeSnap(
    @PrimaryKey val id: Int,
    @ColumnInfo(name = "title") val title: String?,
    @ColumnInfo(name = "comment") val comment: String?,
    @ColumnInfo(name = "date") val date: String?,
    @ColumnInfo(name = "time") val time: String?,
    @ColumnInfo(name = "image") val image: String?,
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readInt(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString()
    )

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(p0: Parcel, p1: Int) {
        p0.writeString(id.toString())
    }


    companion object CREATOR : Parcelable.Creator<FridgeSnap> {
        override fun createFromParcel(parcel: Parcel): FridgeSnap {
            return FridgeSnap(parcel)
        }

        override fun newArray(size: Int): Array<FridgeSnap?> {
            return arrayOfNulls(size)
        }
    }
}