package io.github.drumber.kitsune.data.model.resource

import android.os.Parcelable

sealed class Resource : Parcelable

interface Media : Parcelable {
    val id: String
}
