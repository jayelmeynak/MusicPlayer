# DTO Deezer заполняет Gson рефлексией: поля пишутся по имени, объект создаётся конструктором без
# аргументов. Правила Gson держат только поля с @SerializedName и с переименованием, поэтому поле без
# аннотации в release молча осталось бы пустым. Пакет dto — только модели ответа Deezer.
-keep,allowobfuscation class com.jayelmeynak.lib.network.data.dto.** {
    <init>();
}
-keepclassmembers class com.jayelmeynak.lib.network.data.dto.** {
    <fields>;
}
