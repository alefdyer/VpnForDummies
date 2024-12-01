package com.asinosoft.vpn.api

import android.net.Uri
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.lang.reflect.Type


class ServitorApiFactory {
    fun connect(url: String): ServitorApi {
        val gson = GsonBuilder()
            .registerTypeAdapter(Uri::class.java, UriDeserializer())
            .create()

        val builder = Retrofit.Builder()
            .baseUrl(url)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

        return builder.create(ServitorApi::class.java)
    }

    private class UriDeserializer : JsonDeserializer<Uri> {
        @Throws(JsonParseException::class)
        override fun deserialize(
            json: JsonElement,
            type: Type,
            context: JsonDeserializationContext
        ): Uri = Uri.parse(json.asJsonPrimitive.asString)
    }
}
